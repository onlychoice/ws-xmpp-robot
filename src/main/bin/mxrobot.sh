#!/bin/sh

#
# $RCSfile$
# $Revision: 1194 $
# $Date: 2005-03-30 13:39:54 -0300 (Wed, 30 Mar 2005) $
#

# tries to determine arguments to launch Connection Manager

# Set JVM extra Setting
# JVM_SETTINGS="-Xms512m -Xmx1024m"
JVM_SETTINGS=""

#if cmanager home is not set or is not a directory
if [ -z "$ROBOT_HOME" -o ! -d "$ROBOT_HOME" ]; then
	#resolve links - $0 may be a link in connection_manager's home
	PRG="0"

	# need this for relative symlinks
  	while [ -h "$PRG" ] ; do
    		ls=`ls -ld "$PRG"`
    		link=`expr "$ls" : '.*-> \(.*\)$'`
    		if expr "$link" : '/.*' > /dev/null; then
    			PRG="$link"
    		else
    			PRG=`dirname "$PRG"`"/$link"
    		fi
  	done

	#assumes we are in the bin directory
	ROBOT_HOME=`dirname "$PRG"`/..

	#make it fully qualified
	ROBOT_HOME=`cd "$ROBOT_HOME" && pwd`
fi

#set the CMANAGER_LIB location
ROBOT_LIB="${ROBOT_HOME}/lib"

if [ -z "$JAVACMD" ] ; then
  	if [ -n "$JAVA_HOME"  ] ; then
    		if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      			# IBM's JDK on AIX uses strange locations for the executables
      			JAVACMD="$JAVA_HOME/jre/sh/java"
    		else
      			JAVACMD="$JAVA_HOME/bin/java"
    		fi
  	else
    		JAVACMD=`which java 2> /dev/null `
    		if [ -z "$JAVACMD" ] ; then
        		JAVACMD=java
    		fi
  	fi
fi

if [ ! -x "$JAVACMD" ] ; then
  	echo "Error: JAVA_HOME is not defined correctly."
  	echo "  We cannot execute $JAVACMD"
  	exit 1
fi

CLASS_PATH="";
lib=`ls $ROBOT_LIB`
for line in $lib;do
	# add jar files
	CLASS_PATH=$CLASS_PATH:$ROBOT_LIB/$line
done

# add resources
CLASS_PATH=$CLASS_PATH:$ROBOT_HOME/resources

if [ -z "$LOCALCLASSPATH" ] ; then
	LOCALCLASSPATH=$CLASS_PATH
else
    LOCALCLASSPATH=$CLASS_PATH:$LOCALCLASSPATH
fi

robot_exec_command="exec \"$JAVACMD\" -server $JVM_SETTINGS -classpath \"$LOCALCLASSPATH\" com.netease.xmpp.robot.Robot"
eval $robot_exec_command
