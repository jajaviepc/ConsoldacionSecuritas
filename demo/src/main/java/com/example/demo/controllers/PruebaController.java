package com.example.demo.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.bean.BeanRespuesta;

import jdk.internal.org.xml.sax.SAXException;

@Controller
@RequestMapping("/Javier")
@SuppressWarnings({"unused","resource"})
public class PruebaController {
	
	public static LinkedHashMap<String, String> dicRegEx = new LinkedHashMap<String, String>();
	static {
		//diccionario de expresiones regulares para un tipo especifico de pdf
		dicRegEx.put("fecha", "\\d{1,2}\\/\\d{1,2}\\/\\d{4}");
		dicRegEx.put("comprobante", "[A-Z]{1,2}\\s[0-9]{1,4}-[0-9]{1,8}");
		dicRegEx.put("proveedor", "\\s\\d{1,5}\\s");
//		dicRegEx.put("razonSocial", "[A-z]{1,20}\\s[A-z]{1,20}\\s[A-z]{1,20}");[A-z]{1,20}|
		dicRegEx.put("razonSocial", "([áéíóúa-zA-Z]{3,}[áéíóúa-zA-Z\\s.]{1,})");
		dicRegEx.put("cuit", "[0-9]{10,12}");
		dicRegEx.put("gravado21", "[0-9]+,{1,8}[0-9]{1,2}|-");
	}
	
	@GetMapping("/prueba")
	public String metodoRetorno(Model model) {
		return "index";
	}

	public static void main(String[] args) {
		PruebaController cont = new PruebaController();
		try {
			cont.procesarArchivos();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dummyTest () {
		try {
			/*
			 * Como todo este codigo va a estar en un metodo, cada que entre a este metodo se va  a crear un objeto nuevo que se va a 
			 */
//			String aParsear = "08/01/2013 FA 0001-00000104 1893 Orio S.A. 30707315756 80.000,00               -                              -                            -                                 -                      16.800,00                 -              -                         96.800,00                ";
			String aParsear = "08/01/2013 FA 0002-00001159 4841 Exoparts 30711923698 991,74                     -                              -                            -                                 -                      208,27                       -              -                         1.200,01";
			Pattern patron = null;
			String devolucion = null;
			Matcher match = null;
			String encontrado = null;
			for (String valor : dicRegEx.keySet()) {
				patron = Pattern.compile(dicRegEx.get(valor));
				match = patron.matcher(aParsear);
				while (match.find()) {
					encontrado = aParsear.substring(match.start(), match.end());
					if (valor.equals("fecha")) {
						devolucion = encontrado;
					}else {
						devolucion += ","+encontrado;
					}
				}
			}//Ciclo que recorre todo el String en busca de los
				System.out.println(devolucion);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void toCsv() {
		try {
			final Stream<Path> paths = Files.walk(Paths.get("C:\\Users\\C06016\\Documents\\ProcesadosKorina"));
			final Charset utf8 = Charset.forName("UTF-8");
			paths.filter(Files::isRegularFile).forEach(path->{
				String prueba= null;
				try {
					prueba = path.toAbsolutePath().toString().replace(".txt", ".csv");
				final Scanner scanner = new Scanner(Files.newBufferedReader(path.toAbsolutePath(), utf8));
				PrintWriter pw = new PrintWriter(Files.newBufferedWriter(path.resolve(prueba), utf8, StandardOpenOption.CREATE));
				while (scanner.hasNextLine()) {
		            pw.println(scanner.nextLine());
		        }
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	//Autodetecta la extension del archivo
	private static String detectDocTypeUsingDetector(InputStream stream) throws IOException {
		Detector detector = new DefaultDetector();
		Metadata metadata = new Metadata();
		MediaType mediaType = detector.detect(stream, metadata);
		return mediaType.toString();
	}
	
	//Pasamanos para detectar la extension del archivo
	private void detectorXtension () throws IOException {
		String ruta = "";
		File file = new File (ruta);
		InputStream stream = new FileInputStream(file);
		String mediaType = detectDocTypeUsingDetector(stream);
	    stream.close();
	}
	
	// Extrae el contenido del archivo que se encuentre en los recursos del proyecto(Es un pasamanos)
	private void extractorContenido(String ruta) throws Exception {
		File file = new File(ruta);
		InputStream stream = new FileInputStream(file);
		String contenidoPDF = extractContentUsingParser(stream);
		String[] lineascontenidoPDF = contenidoPDF.split("\n");
		escribirArchivoTXT(lineascontenidoPDF,file.getName());
	}
	
	// Escribe en un archivo txt el contenido del pdf
	private static void escribirArchivoTXT(String[] data,String nameArch) throws IOException {
		nameArch=nameArch.replace(".pdf", "");
		FileWriter write = new FileWriter("C:\\Users\\C06016\\Documents\\ProcesadosKorina\\"+nameArch+".csv");
		PrintWriter printLine = new PrintWriter(write);
		for (int i = 1; i < data.length; i++) {
			// Llamo a tratamientoTexto para poder generar el archivo txt y luego el csv
			printLine.printf("%s", tratamientoTexto(data[i]) + "\n");
			i++;
		}

		printLine.close();
	}

	// Extrae propiamente el contenido del pdf haciendo uso de la libreria TIKA
	private static String extractContentUsingParser(InputStream stream)
			throws IOException, TikaException, SAXException, org.xml.sax.SAXException {
		Parser parser = new AutoDetectParser();
		BodyContentHandler handler = new BodyContentHandler(-1);// Se le pasa -1 para que lea mas de 10k caracteres
		Metadata metadata = new Metadata();
		ParseContext context = new ParseContext();
		parser.parse(stream, handler, metadata, context);
		return handler.toString();
	}
	
	
	private static String tratamientoTexto(String aParsear) {
		try {
			Pattern patron = null;
			String devolucion = null;
			Matcher match = null;
			String encontrado = null;
			for (String valor : dicRegEx.keySet()) {
				patron = Pattern.compile(dicRegEx.get(valor));
				match = patron.matcher(aParsear);
				while (match.find()) {
					encontrado = aParsear.substring(match.start(), match.end());
					if (valor.equals("fecha")) {
						devolucion = "\"" + encontrado + "\"";
					} else {
						devolucion += ",\"" + encontrado + "\"";
					}
				}
			} // Ciclo que recorre todo el String en busca de los
			return devolucion;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public void procesarArchivos() {
		Stream<Path> paths;
		try {
			paths = Files.walk(Paths.get("C:\\Users\\C06016\\Documents\\CosoKorina"));

			List<String> result = paths.filter(Files::isRegularFile).map(ruta -> ruta.toString())
					.collect(Collectors.toList());
			result.forEach(name -> {
				try {
					extractorContenido(name);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
