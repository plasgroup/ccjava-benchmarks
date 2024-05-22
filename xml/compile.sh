if [ x"$JAVA_HOME" = x ]; then
  JAVAC=javac
else
  JAVAC=$JAVA_HOME/bin/javac
fi

exec $JAVAC -d bin src/Main.java

