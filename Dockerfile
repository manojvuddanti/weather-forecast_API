# Use an OpenJDK image as the base for running the Java application
FROM openjdk:19-jdk-alpine

# Set the working directory for the Java application
WORKDIR /app

# Copy the JAR file to the working directory
COPY target/unixproject.jar /app/project.jar

# Install NGINX to serve your HTML file
RUN apk update && apk add nginx

# Remove the default NGINX configuration
RUN rm /etc/nginx/http.d/default.conf

# Copy the NGINX configuration file into the container
COPY nginx.conf /etc/nginx/http.d/default.conf

# Copy the HTML file from the specified location to NGINX's root directory
COPY src/com/api/index.html /usr/share/nginx/html/

# Expose the ports that NGINX and the Java application will run on
EXPOSE 80 8089

# Start NGINX and the Java application
CMD ["sh", "-c", "nginx && java -jar project.jar"]
