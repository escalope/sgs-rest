/*
	This file is part of SGSim-REST framework, a game to learn coordination protocols with microgrids
	
    Copyright (C) 2017 Rafael Pax, Jorge J. Gomez-Sanz

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.sgsimulator.sgsrest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import com.github.aesteve.vertx.nubes.VertxNubes;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class SGSVertxServer {

	// from
	// http://stackoverflow.com/questions/31080215/getting-the-ip-address-of-eth0-interface-in-java-only-return-ipv6-address-and-no
	public static String getInterfaceIP() throws SocketException
	{
		String ip = "";
		String interfaceName = "enp2s0";
		NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
		Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
		InetAddress currentAddress;
		currentAddress = inetAddress.nextElement();
		while (inetAddress.hasMoreElements())
		{
			currentAddress = inetAddress.nextElement();
			if (currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress())
			{
				ip = currentAddress.toString();
				break;
			}
		}
		return ip;
	}

	public static void main(String[] args) throws Exception
	{
		String configFile = null;
		if (args.length == 0)
		{
			configFile = "config.json";
		} else
		{
			configFile = args[0];
		}
		
		Path configPath = Paths.get(configFile);
		JsonObject config = new JsonObject(new String(Files.readAllBytes(configPath)));
		
		Random r = new Random(new Date().getTime());
		char c1 = (char)(r.nextInt(26) + 'a');
		char c2 = (char)(r.nextInt(26) + 'a');
		char c3 = (char)(r.nextInt(26) + 'a');
		String admincode=""+c1+c2+c3;
		System.getProperties().put("sgsimulator.admincode", admincode);
		
		Vertx vertx = Vertx.vertx();
		System.out.println("****************************");
		System.out.println("Starting...");
		System.out.println("To load pages:");
		System.out.println("http://" + config.getString("host") + ":" + config.getInteger("port")
				+ "/sg/pages/panel");
		System.out
				.println("Admin interface (only allowed from " + config.getString("adminip") + ":");
		System.out.println("http://" + config.getString("host") + ":" + config.getInteger("port")
				+ "/sg/pages/admin/"+admincode);
		System.out.println("Projector screen interface (only allowed from "
				+ config.getString("adminip") + ":");
		System.out.println("http://" + config.getString("host") + ":" + config.getInteger("port")
				+ "/sg/pages/screen");

		System.out.println("****************************");

		VertxNubes nubes = new VertxNubes(vertx, config);

		// System.err.println("IP:"+ip.substring(1));

		// config.put("host", ip.substring(1));
		System.getProperties().put("sgsimulator.scenario",
				config.getJsonObject("gridlab").getString("scenario"));
		System.getProperties().put("sgsimulator.adminip", config.getString("adminip"));
		nubes.bootstrap(res -> {
			if (res.succeeded())
			{
				final Router router = res.result();
				final HttpServer server = vertx
						.createHttpServer(new HttpServerOptions(config));
				server.requestHandler(router::accept);
				server.listen();
				System.out.println("****************************");
				System.out.println("Started...");
				System.out.println("****************************");
			} else
			{
				res.cause().printStackTrace();
			}
		});
		// nubes.stop((_void) -> {
		// vertx.close();
		// });
	}

}
