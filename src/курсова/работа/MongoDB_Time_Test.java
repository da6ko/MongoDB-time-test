/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package курсова.работа;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.json.JSONException;

public class КурсоваРабота {

    final static String DATABASE_NAME = "КурсоваРабота";
    final static String COLLECTION_NAME = "Сензори";
    final static int NUM_OF_DOCS = 5;
    final static int NUM_OF_SENSORS = 50;

    // Променливи за работа на потребителя с различните видове тестове
    final static String SINGLE = "ЕДИНИЧНИ";
    final static String BLOCK = "БЛОКОВИ";
    final static String CONSTANT_TIMER = "ЕТАЛОННО";
    final static String RANDOM_TIMER = "СЛУЧАЙНО";

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {

        String sensorTypes[] = {"Температура", "Влажност", "Атмосферно налягане"};
        String sensorUnits[] = {"\u00B0C", "%", "hPa"};
        int sensorMinValues[] = {-50, 1, 1011};
        int sensorMaxValues[] = {50, 99, 1015};
        int numberOfSensorTypes = sensorTypes.length;

        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);

        // Въпроси към потребителя
        System.out.println("Въведете типа заявки - единични или блокови");
        BufferedReader cyrillicBuffer = new BufferedReader(new InputStreamReader(System.in, "Cp1251"));
        String typeOfRequest = cyrillicBuffer.readLine();

        while (!typeOfRequest.equalsIgnoreCase(SINGLE) && !typeOfRequest.equalsIgnoreCase(BLOCK)) {
            System.out.println("Въведени са невалидни стойности. Моля изберете единични или блокови");
            typeOfRequest = cyrillicBuffer.readLine();
        }

        System.out.println("Изберете дали заявките да бъдат изпълнявани през еталонно или случайно време");
        String typeOfInsert = cyrillicBuffer.readLine();
        while (!typeOfInsert.equalsIgnoreCase(CONSTANT_TIMER) && !typeOfInsert.equalsIgnoreCase(RANDOM_TIMER)) {
            System.out.println("Въведени са невалидни стойности. Моля изберете еталонно или случайно време");
            typeOfInsert = cyrillicBuffer.readLine();
        }

        // Връзка с база данни и изпълнение на зададената задача
        try {
            System.out.println("Свързване с MongoDB Atlas ... ");
            MongoClient mongoClient = new MongoClient(
                    new MongoClientURI(
                            "mongodb+srv://Admin:123456a@testcluster.ehr4d.mongodb.net/test?authSource=admin&replicaSet=atlas-13u9s5-shard-0&readPreference=primary&appname=MongoDB%20Compass&ssl=true"
                    )
            );
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            System.out.println("Премахване на стари документи ... ");
            long t1 = System.nanoTime();
            deleteDocuments(collection, t1);

            System.out.println("Добавяне на нови документи ... ");
            t1 = System.nanoTime();
            if (typeOfRequest.equalsIgnoreCase(SINGLE)) {
                insertSingleDocuments(collection, sensorTypes, sensorUnits, sensorMinValues,
                        sensorMaxValues, numberOfSensorTypes, typeOfInsert);
            }
            if (typeOfRequest.equalsIgnoreCase(BLOCK)) {
                insertBlockDocuments(collection, sensorTypes, sensorUnits, sensorMinValues,
                        sensorMaxValues, numberOfSensorTypes, typeOfInsert);
            }
            long t2 = System.nanoTime();
            long documentInsertTime = (t2 - t1) / 1000000;
            System.out.println("Общото време добавяне беше: " + documentInsertTime + "ms");

        } catch (JSONException e) {
            System.out.println(e);
        }
        // ръчно прекратяване на програмата, нужно заради заспиването на нишката
        System.exit(0);
    }

    public static void insertSingleDocuments(MongoCollection<Document> collection, String sensorTypes[], String sensorUnits[],
            int sensorMinValues[], int sensorMaxValues[], int numberOfSensorTypes, String typeOfInsert) {

        int counter = 0;

        while (counter < NUM_OF_DOCS) {

            int sensorId = getRandomNumberInRange(1, NUM_OF_SENSORS);
            int sensorTypeId = getRandomNumberInRange(0, numberOfSensorTypes - 1);
            String sensorTypeName = sensorTypes[sensorTypeId];
            String units = sensorUnits[sensorTypeId];
            int minValue = sensorMinValues[sensorTypeId];
            int maxValue = sensorMaxValues[sensorTypeId];
            int sensorValue = getRandomNumberInRange(minValue, maxValue);

            int year = getRandomNumberInRange(2018, 2021);
            int month = getRandomNumberInRange(1, 12);
            int day = getRandomNumberInRange(1, 31);
            int hour = getRandomNumberInRange(0, 23);
            int min = getRandomNumberInRange(0, 59);
            int sec = getRandomNumberInRange(0, 59);

            String record = String.format("{"
                    + "\"sensorId\": %d,"
                    + "\"timestamp\": {"
                    + "\"$date\": \"%04d-%02d-%02dT%02d:%02d:%02dZ\""
                    + "},"
                    + "\"type\": \"%s\","
                    + "\"value\": %d,"
                    + "\"units\": \"%s\""
                    + "}", sensorId, year, month, day, hour, min, sec,
                    sensorTypeName, sensorValue, units);
            try {
                long insertTime = System.nanoTime();
                Document doc = Document.parse(record);
                collection.insertOne(doc);
                long estimatedTime = (System.nanoTime() - insertTime) / 1000000;

                // пауза спрямо типа време
                if (typeOfInsert.equalsIgnoreCase(CONSTANT_TIMER)) {
                    
                    wait(1000);
                } else {
                    wait(getRandomMilisecond(250, 5000));
                }
                counter++;
                System.out.println("Документ " + counter + " добавен за " + (estimatedTime) + "ms");

            } catch (JsonParseException e) {
            }
        }

    }

    public static void insertBlockDocuments(MongoCollection<Document> collection, String sensorTypes[], String sensorUnits[],
            int sensorMinValues[], int sensorMaxValues[], int numberOfSensorTypes, String typeOfInsert) {

        int counter = 0;
        List<Document> list = new ArrayList<Document>();

        while (counter < 5) {

            int sensorId = getRandomNumberInRange(1, NUM_OF_SENSORS);
            int sensorTypeId = getRandomNumberInRange(0, numberOfSensorTypes - 1);
            String sensorTypeName = sensorTypes[sensorTypeId];
            String units = sensorUnits[sensorTypeId];
            int minValue = sensorMinValues[sensorTypeId];
            int maxValue = sensorMaxValues[sensorTypeId];
            int sensorValue = getRandomNumberInRange(minValue, maxValue);

            int year = getRandomNumberInRange(2018, 2021);
            int month = getRandomNumberInRange(1, 12);
            int day = getRandomNumberInRange(1, 31);
            int hour = getRandomNumberInRange(0, 23);
            int min = getRandomNumberInRange(0, 59);
            int sec = getRandomNumberInRange(0, 59);
            String test = String.format("\"%04d-%02d-%02d: %02d:%02d:%02d\"", year, month, day, hour, min, sec);

            Document sensorInfo = new Document("sensorId", sensorId)
                    .append("timestamp", test)
                    .append("type", sensorTypeName)
                    .append("value", sensorValue)
                    .append("units", units);

            // пауза спрямо типа време
            if (typeOfInsert.equalsIgnoreCase(CONSTANT_TIMER)) {
                wait(1000);
            } else {
                wait(getRandomMilisecond(250, 5000));
            }

            // добавяне на информацията в блок от типа List
            list.add(sensorInfo);
            counter++;
        }
        try {
            collection.insertMany(list);
        } catch (JsonParseException e) {
        }
    }

    private static int getRandomNumberInRange(int min, int max) {
        Random randomGen = new Random();
        int randomValue = min + randomGen.nextInt(max - min + 1);
        return randomValue;
    }

    // метод за генериране на време за изчакване на случаен интервал
    private static int getRandomMilisecond(int min, int max) {
        Random randomGen = new Random();
        int randomValue = min + randomGen.nextInt(max - min + 1);
        return randomValue;
    }

    // метод за изчакване, което се получава от заспиване на нишката за определено време
    public static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void deleteDocuments(MongoCollection<Document> collection, long t1) {
        long n = collection.countDocuments();
        if (n != 0) {
            collection.deleteMany(new Document());
        }
    }
}
