@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
"%JAVA_HOME%\bin\java.exe" -jar backend\target\attendance-0.1.0.jar --server.port=8088 --spring.datasource.url="jdbc:postgresql://ep-silent-scene-azituv9l-pooler.c-3.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require" --spring.datasource.username=neondb_owner --spring.datasource.password=npg_pdDHSQxqG48X
