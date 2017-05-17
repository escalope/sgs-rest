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

package net.sf.sgsimulator.sgsrest.vertx.controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.aesteve.vertx.nubes.annotations.Controller;
import com.github.aesteve.vertx.nubes.annotations.params.Param;
import com.github.aesteve.vertx.nubes.annotations.routing.http.GET;
import com.github.aesteve.vertx.nubes.annotations.services.Service;

import info.macias.sse.vertx3.VertxEventTarget;
import io.vertx.ext.web.RoutingContext;
import net.sf.sgsimulator.sgsrest.vertx.services.GridLabSimulatorService;

@Controller("/sg/assets")
public class AssetsController {

	@Service("GridLabSimulatorService")
	private GridLabSimulatorService gridLab;
	
	@GET("/:assetname")
	public String getAsset(
			@Param(value = "assetname", mandatory = true) String file)
	{
		
		if (!file.matches("^[\\w,\\s-]+\\.([A-Za-z]|[A-Za-z][A-Za-z]|[A-Za-z][A-Za-z][A-Za-z])$"))
			return "Invalid file name "+file;
		String fileName="web/assets/"+file;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

			String line;
			StringBuffer filecontent=new StringBuffer();
			while ((line = br.readLine()) != null) {
				filecontent.append(line+"\n");
			}

			return filecontent.toString();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Failure loading "+file;
	}

	

}
