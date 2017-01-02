FROM fabric8/java-jboss-openjdk8-jdk:1.2.1

ENV JAVA_APP_JAR api-gateway.jar
ENV AB_ENABLED off
ENV AB_JOLOKIA_AUTH_OPENSHIFT true
ENV JAVA_OPTIONS -Xmx256m -Djava.security.egd=file:///dev/./urandom
ENV ENABLE_ZIPKIN false

ENV ZIPKIN_SERVER_URL http://zipkin-query:9411

EXPOSE 8080

ADD target/api-gateway.jar /deployments/