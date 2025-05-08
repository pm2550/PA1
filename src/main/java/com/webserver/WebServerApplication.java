package com.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.webserver.ServerConfig;




@SpringBootApplication
public class WebServerApplication {

    public static void main(String[] args) {


        for(int i=0;i<args.length;i++) {
            if(args[i].equals("-document_root")){
                System.setProperty("document.root", args[i+1]);
            }
                else if(args[i].equals("-port")){
                    System.setProperty("server.port", args[i+1]);
                }
            }

            SpringApplication.run(WebServerApplication.class, args);
        }
        }
  
