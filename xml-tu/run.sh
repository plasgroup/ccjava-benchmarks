if [ x"$JAVA_HOME" = x ]; then
  JAVA=java
else
  JAVA=$JAVA_HOME/bin/java
fi

exec $JAVA -Xms5000m -Xmx5000m -cp bin Main $* data/psd7003.xml

