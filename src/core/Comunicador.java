package core;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Comunicador {
	private static Gson gson = new GsonBuilder().create();
	private static JsonParser parser = new JsonParser();
	private static final boolean DEBUG = true;
	
	public static Gson getGSON() {
		return gson;
	}
	
	public static <T> void send(ObjectOutputStream out, Mensagem<T> data) throws IOException {
		String jsonString = getGSON().toJson(data);
		out.writeUTF(jsonString);
		out.flush();
		if (DEBUG) {
			System.out.println("-------------------[DEBUG]-------------------");
			System.out.println ("    Enviada a mensagem "+getGSON().toJson(data)+"...");
			System.out.println("---------------------------------------------");
		}
	}
	
	public static JsonObject receive(ObjectInputStream in) throws IOException {
		String message;
		message = in.readUTF();
		if (message == null) return null;
		JsonObject data = parser.parse(message).getAsJsonObject();
		/*
		Mensagem<T> data = getGSON().fromJson(message, new TypeToken<Mensagem<T>>() {}.getType());
		*/
		if (DEBUG) {
			System.out.println("-------------------[DEBUG]-------------------");
			System.out.println ("    Recebida a mensagem ["+data.get("header")+"] "+data.get("dado").toString());
			System.out.println("---------------------------------------------");
		}
		return data;
	}
	
	public static Header getHeader(JsonObject obj) {
		return getGSON().fromJson(obj.get("header"), Header.class);
	}
	
	public static <T> T getDado(JsonObject obj, Class<T> C) {
		return getGSON().fromJson(obj.get("dado"), C);
	}
	
	public static <T> T getDado(JsonObject obj, Type TypeOfC) {
		return getGSON().fromJson(obj.get("dado"), TypeOfC);
	}
}
