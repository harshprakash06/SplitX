package com.xtemper.connectme.Database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDB {
    private static final MongoClient mongoClient;
    private static final MongoDatabase database;

    // Initialize MongoDB connection once
    static {
        String connectionString = ""; //database url


        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(pojoCodecRegistry)
                .serverApi(serverApi)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("SplitX"); // Replace with your actual DB name
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
