package com.reader.controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLRequest;

@Controller
public class HomeController 
{
	boolean isSuccess = false;
	boolean canDownload = false;
	String cap = "";
	
    @GetMapping("/")
    public String home(Model model) 
    {
        return "index";
    }

    @PostMapping("/getcaptions")
    public String uploadFile(@RequestParam("vidURL") String vidURL, RedirectAttributes redirectAttributes)
    {
    	String captions = captionScraper(vidURL);
    	
    	if(isSuccess)
    	{
    		canDownload = true;
    		redirectAttributes.addFlashAttribute("successmessage", vidURL + " accepted!");
    		redirectAttributes.addFlashAttribute("message", captions);
    	}
    	else
    	{
    		canDownload = false;
    		cap = "";
    		redirectAttributes.addFlashAttribute("errormessage", "Unable to grab transcripts for " + vidURL);
    		redirectAttributes.addFlashAttribute("message", "(captions unavailable for this video)");
    	}
    	return "redirect:/";
    }  
    
    @PostMapping("/download")
    public String downloadFile3(RedirectAttributes redirectAttributes) 
    {
    	if(canDownload)
    	{
        	String home = System.getProperty("user.home");
        	PrintWriter writer;
    		try 
    		{
    			writer = new PrintWriter(home + "/Downloads/captions.txt", "UTF-8");
    	    	writer.println(cap);
    	    	writer.close();
    	    	redirectAttributes.addFlashAttribute("file_success", "File downloaded to your downloads folder!");
    		} 
    		catch (FileNotFoundException e) 
    		{
    			redirectAttributes.addFlashAttribute("file_err", "Unable to download file.");
    		} 
    		catch (UnsupportedEncodingException e) 
    		{
    			redirectAttributes.addFlashAttribute("file_err", "Unable to download file.");
    		}
    	}
    	else
    	{
    		redirectAttributes.addFlashAttribute("file_err", "Unable to download file.");
    	}

    	return "redirect:/";
    }
    
    public String captionScraper(String vidURL)
    {
    	vidURL = vidURL.replaceAll("\\s+","");
    	String capURL = "";
    	String parsed_captions = "";
    	
    	if(vidURL.toLowerCase().indexOf("https://miamifl.granicus.com/MediaPlayer.php".toLowerCase()) != -1)
    	{
    		String id = vidURL.substring(vidURL.lastIndexOf("=") + 1);
    		capURL = "https://miamifl.granicus.com/videos/" + id + "/captions.vtt";
    		String tmp = readCaptionsSite(capURL);
    		cap = tmp;
    		return tmp;
    	}
    	else if(vidURL.toLowerCase().indexOf("https://apps.tampagov.net/cttv_cc_webapp/Agenda.aspx".toLowerCase()) != -1)
    	{
    		return readCaptionsSite(vidURL);
    	}
    	else if(vidURL.toLowerCase().indexOf("http://miamifl.iqm2.com/Citizens/SplitView.aspx".toLowerCase()) != -1)
    	{
    		String content = null;
    		URLConnection connection = null;
    		
    		try 
    		{
	    		  connection =  new URL(vidURL).openConnection();
	    		  Scanner scanner = new Scanner(connection.getInputStream());
	    		  scanner.useDelimiter("\\Z");
	    		  content = scanner.next();
	    		  
	    		  content = content.substring(content.indexOf("/Services/TranscriptGet.aspx?MediaID=") + 1);
	    		  content = content.substring(0, content.indexOf("&format=vtt"));
	    		  String id = content.substring(content.lastIndexOf("=") + 1);
	    		  scanner.close();
	    		  
		      	  String tmp = readCaptionsSite("http://miamifl.iqm2.com/Services/TranscriptGet.aspx?MediaID=" + id + "&format=vtt");
		      	  cap = tmp;
		      	  isSuccess = true;
		      	  return tmp;
    		}
    		catch (Exception ex ) 
    		{
    			isSuccess = false;
    		    return null;
    		}
    	}
    	else if(vidURL.toLowerCase().indexOf("http://coralgables.granicus.com/MediaPlayer.php".toLowerCase()) != -1)
    	{
    		String id = vidURL.substring(vidURL.lastIndexOf("=") + 1);
    		capURL = "http://coralgables.granicus.com/videos/" + id + "/captions.vtt";
    		String tmp = readCaptionsSite(capURL);
    		cap = tmp;
    		return tmp;
    	}
    	else if(vidURL.toLowerCase().indexOf("http://miamidade.granicus.com/MediaPlayer.php".toLowerCase()) != -1)
    	{
    		String id = vidURL.substring(vidURL.lastIndexOf("=") + 1);
    		capURL = "http://miamidade.granicus.com/videos/" + id + "/captions.vtt";
    		String tmp = readCaptionsSite(capURL);
    		cap = tmp;
    		return tmp;
    	}
    	else if(vidURL.toLowerCase().indexOf("https://pub-miamilakes.escribemeetings.com/Players/ISIStandAlonePlayer.aspx?ClientId=miamilakes&FileName=New".toLowerCase()) != -1)
    	{
    		String id = vidURL.substring(vidURL.lastIndexOf("Encoder") + 7);
    		capURL = "https://video.isilive.ca/miamilakes/New%20Encoder" + id + ".vtt";
    		try 
    		{
        		InputStream inputStream = new URL(capURL).openStream();
        		parsed_captions = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        		cap = parsed_captions;
        		isSuccess = true;
        		return parsed_captions;
			} 
    		catch (Exception e) 
    		{
    			isSuccess = false;
				return null;
			}
    	}
    	else if(vidURL.toLowerCase().indexOf("https://www.youtube.com/watch?v=".toLowerCase()) != -1)
    	{
    		parsed_captions = youtubeCaptions(vidURL);
    		cap = parsed_captions;
    		return parsed_captions;
    	}
    	else
    	{
    		isSuccess = false;
    		return null;
    	}
    }
       
    public String readCaptionsSite(String capURL)
    {
        try 
        {
            Document doc = Jsoup.connect(capURL).get();
            String parsed_captions = doc.body().text();
            isSuccess = true;
    		cap = parsed_captions;
    		return parsed_captions;
        } 
        catch (Exception e) 
        {
        	isSuccess = false;
            return null;
        }
    }
    
    public String youtubeCaptions(String capURL)
    {
    	// destination directory
    	String directory = System.getProperty("user.home");
    	String id = capURL.substring(capURL.lastIndexOf("=") + 1);

    	// build request
    	YoutubeDLRequest request = new YoutubeDLRequest("--write-sub --write-auto-sub --skip-download " + capURL, directory);
    	request.setOption("ignore-errors");		// --ignore-errors
    	request.setOption("output", "%(id)s");	// --output "%(id)s"
    	request.setOption("retries", 10);		// --retries 10

    	// make request and return response
		try 
		{
			YoutubeDL.execute(request);
			String captions  = readFile(directory + "/" + id + ".en.vtt");
			Files.deleteIfExists(Paths.get(directory + "/" + id + ".en.vtt")); 
	    	isSuccess = true;
	        return captions;
		} 
		catch (Exception e) 
		{
			isSuccess = false;
			return null;
		}    
		
    }
    
    // reads Youtube vtt files
    private static String readFile(String filePath) 
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) 
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

}



