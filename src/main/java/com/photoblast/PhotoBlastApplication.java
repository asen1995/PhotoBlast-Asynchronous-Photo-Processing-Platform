package com.photoblast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Main entry point for the PhotoBlast application.
 * <p>
 * PhotoBlast is an asynchronous photo processing platform that handles
 * user-uploaded photos through a distributed queue-based architecture using RabbitMQ.
 * </p>
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class PhotoBlastApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhotoBlastApplication.class, args);
    }

}
