package bartkoo98.com.github.ezapytanie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class EZapytanieApplication {

    public static void main(String[] args) {
        SpringApplication.run(EZapytanieApplication.class, args);
    }

}
