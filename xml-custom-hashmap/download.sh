mkdir data
cd data
curl -O http://aiweb.cs.washington.edu/research/projects/xmltk/xmldata/data/pir/psd7003.dtdes

curl -O http://aiweb.cs.washington.edu/research/projects/xmltk/xmldata/data/pir/psd7003.xml
cd ..
curl -O https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-ri/2.3.1/jaxb-ri-2.3.1.zip
unzip jaxb-ri-2.3.1.zip
curl -O https://repo1.maven.org/maven2/com/sun/activation/javax.activation/1.2.0/javax.activation-1.2.0.jar
mv javax.activation-1.2.0.jar jaxb-ri/mod

