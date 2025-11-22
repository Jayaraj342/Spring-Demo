FROM tomcat:10.1-jdk17-temurin

# Remove default ROOT app
RUN rm -rf /usr/local/tomcat/webapps/ROOT*

# Copy your WAR file as ROOT.war
COPY target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
