package com.netease.xmpp.robot.c2s;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.util.StringUtils;

import com.netease.xmpp.robot.Robot;

public class ClientPacketListener implements PacketListener {
    private static Logger logger = Logger.getLogger(ClientPacketListener.class);

    private static final String USER_PARA = "x_auth_username";

    private static final String URL_PREFIX = "<url>";
    private static final String URL_SURFIX = "</url>";

    private static final String PARAMS_PREFIX = "<params>";
    private static final String PARAMS_SURFIX = "</params>";

    private static final String DWR_PREFIX = "<dwr xmlns=\"netease:ajax:dwr\"><![CDATA[";
    private static final String DWR_SURFIX = "]]></dwr>";

    /** Default initial size of the response buffer if content length is unknown. */
    private static final int DEFAULT_INITIAL_BUFFER_SIZE = 4 * 1024; // 4 kB

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    class DWRPacket implements PacketExtension {
        private String content = null;

        public DWRPacket(String content) {
            this.content = content;
        }

        @Override
        public String getElementName() {
            return "dwr";
        }

        @Override
        public String getNamespace() {
            return "netease:ajax:dwr";
        }

        @Override
        public String toXML() {
            StringBuilder sb = new StringBuilder(DWR_PREFIX);
            sb.append(content);
            sb.append(DWR_SURFIX);

            return sb.toString();
        }

    }

    class RequestTask implements Runnable {
        private String jid = null;
        private String user = null;
        private String message = null;
        private String appServerAddr = null;

        public RequestTask(String jid, String message) {
            this.jid = jid;
            String user = StringUtils.parseName(jid);
            this.user = user.replace("\\40", "@");
            this.message = message;
            // this.appServerAddr = Robot.getInstance().getAppServerAddr();
        }

        @Override
        public void run() {
            HttpClient client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);

            try {
                String encodedUrl = message.substring(message.indexOf(URL_PREFIX)
                        + URL_PREFIX.length(), message.indexOf(URL_SURFIX));
                String params = message.substring(message.indexOf(PARAMS_PREFIX)
                        + PARAMS_PREFIX.length(), message.indexOf(PARAMS_SURFIX));

                params = params.replaceAll("&amp;", "\n");

                String url = URLDecoder.decode(encodedUrl, "UTF-8");

                PostMethod method = new PostMethod(url);
                RequestEntity entity = new StringRequestEntity(params, "text/plain", null);
                method.setRequestEntity(entity);
                method.setRequestHeader(USER_PARA, user);

                int statusCode = client.executeMethod(method);
                logger.debug(">>> Send msg: " + message);
                if (statusCode == HttpStatus.SC_OK) {
                    InputStream stream = method.getResponseBodyAsStream();
                    long contentLength = method.getResponseContentLength();
                    String charset = method.getResponseCharSet();

                    ByteArrayOutputStream outstream = new ByteArrayOutputStream(
                            contentLength > 0 ? (int) contentLength : DEFAULT_INITIAL_BUFFER_SIZE);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        outstream.write(buffer, 0, len);
                    }
                    outstream.close();

                    String result = outstream.toString(charset);

                    logger.debug(">>> Response: " + result);

                    Robot.getInstance().getServerSurrogate().send(new DWRPacket(result), jid, false);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void processPacket(Packet packet) {
        Message msg = (Message) packet;
        String jid = msg.getFrom();
        String content = msg.getBody();

        logger.debug(">>> Recv msg from: " + jid + ": " + content);

        threadPool.execute(new RequestTask(jid, content));
    }
}
