// globals
var companyName = "t-a-l-e-s";
var port = 8080;
var maxLogSize = 20000;
var logData = [];
var paused = false;
var deadInterval = 30000;
var errorsInterval = 10000;
var startInterval = 10000;
var checkDatabaseInterval = 10000;
var appsInterval = {};
var serversInterval = {};
var colors = ["74A6BD", "7195A3", "EB8540", "AB988B", "628B61", "CE7898", "66CCFF", "D6A354", "0099CC", "9CB770", "A84A5C", "CC99CC", "EC633F"]
var colorIndex = 0;


jQuery.fn.exists = function(){return this.length>0;}


$(document).ready(function() {
	
	$("h1").text(companyName);
	
	function uintToString(uintArray) {
	    var encodedString = String.fromCharCode.apply(null, uintArray),
	        decodedString = decodeURIComponent(escape(encodedString));
	    return decodedString;
	}
	
	function start(){
	
		var location = document.location.toString().replace('http://','ws://').replace('https://', 'wss://').replace("#", "");
		var ws = new WebSocket(location);
		ws.binaryType = 'arraybuffer';
		
		ws.onopen = function() {
			
			$("title").text(companyName);
			$("h1").text($("title").text());
			$("body").css("background-color", "#CCC");
			
		};
		
        ws.onmessage = function(data) {
	alert("--" + data.data)
	var c = new Uint8Array(data.data, 0, data.data.byteLength)
	alert(c.byteOffset)
	alert(c.length)
	var gunzip = new Zlib.Gunzip(c.buffer);
	var plain = gunzip.decompress();
	
	alert(plain.length)
	alert(uintToString(plain))
	alert(uintToString(plain).length)

			if(!paused){
			
				//data = JSON.parse(data.data);
				data = [JSON.parse(data.data)];
		
				for(var i = 0; i < data.length; i++){
				
					json = data[i];

					var classe = getClassName(json.methodPath);
					var pid = json.pid;	
					var publicDNS = json.publicDNS;			
					var serverDivName = publicDNS.split(".").join("_");
					var appDivName = "tales_" + serverDivName + "_" + pid;
					var process = json.process;

			
					// checks if server div exists
					if(!$("#" + serverDivName).exists()){
						
						if(colorIndex >= colors.length){
							colorIndex = 0;
						}
						
						$("#analytics").prepend("<div id='" + serverDivName +"' class='serverContainer'></div>");
						$("#" + serverDivName).css("background-color", "#" + colors[colorIndex++]);

						// server title
						$("#" + serverDivName).append("<hr><h2><a href=\"http://" + publicDNS + ":" + port + "\" target=\"_blank\">" + publicDNS + ":" + port + "</a> | <a href=\"#\" onclick=\"deleteServer('" + publicDNS + "')\">delete</a></h2>");	
					
						// stats container
						$("#" + serverDivName).append("<div id=\"stats\"></div>");
						
						// server status
						$("#" + serverDivName).find("#stats").append("<div id=\"server\"><h2>Server Info</h2></div>");
						$("#" + serverDivName).find("#stats").find("#server").append("<div id=\"stats\"></div>");
				
						// apps
						$("#" + serverDivName).append("<div id=\"apps\"></div>");
						
						// database div
						if(!$("#databases_" + serverDivName).exists()){
							$("#databasesStats").prepend("<div id='databases_" + serverDivName +"' class='databaseContainer'></div>");
							$("#databases_" + serverDivName).css("background-color", "#" + colors[colorIndex++]);
						}
						
						checkDatabasesInfo(publicDNS, $("#databases_" + serverDivName));


					}else{
						clearTimeout(serversInterval[serverDivName]);
					}
					
					
					setServerRemoveTimeout(serverDivName);

					
					if(classe == "ServerMonitor"){
						
						
						setServerStats($("#" + serverDivName).find("#server").find("#stats"), json.data);
				
				
					}else{


						// check if the app div exists
						if(!$("#" + appDivName).exists()){

							$("#" + serverDivName).find("#apps").prepend("<div id='" + appDivName + "' class='appContainer'></div>");

							$("#" + appDivName).append("<hr />");
							$("#" + appDivName).append("<h3>" + process + " | pid " + pid + " | <a href=\"#\" onclick=\"kill(" + pid + ", '" + publicDNS + "')\">kill</a></h3>");
							$("#" + appDivName).append("<div id=\"custom\"></div>");

							// template log
							$("#" + appDivName).append("<h4>log</h4>");
							$("#" + appDivName).append("<textarea id=\"templateLog\" wrap='off' class='logTextfield'></textarea>");	

							// error log
							$("#" + appDivName).append("<h4>error log</h4>");
							$("#" + appDivName).append("<textarea id=\"errorLog\" wrap='off' class='logTextfield'></textarea>");

							// full log
							$("#" + appDivName).append("<h4>full log</h4>");
							$("#" + appDivName).append("<textarea id=\"log\" wrap='off' class='logTextfield'></textarea>");					

						}else{
							clearTimeout(appsInterval[appDivName]);
						}


						setAppRemoveTimeout(appDivName);
			

						// inits the log acumm
						if(logData[appDivName] == undefined){
							logData[appDivName] = {};
							logData[appDivName]["templateLog"] = "";
							logData[appDivName]["errorLog"] = "";
							logData[appDivName]["log"] = "";
						}


						// prints the log
						header = new Date().getHours() + ":" + new Date().getMinutes() + ":" + new Date().getSeconds() + " | " + json.data;

						logData[appDivName]["log"] = (header + "\n" + logData[appDivName]["log"]).substring(0, maxLogSize);;
						$("#" + appDivName + " #log").val(logData[appDivName]["log"]);


						// template log
						if (classe != "AppMonitor" && classe != "TaskWorker" && classe.indexOf("Scraper") == -1 && json.data.indexOf("[ERROR START]") == -1){
							logData[appDivName]["templateLog"] = (header + "\n" + logData[appDivName]["templateLog"]).substring(0, maxLogSize);;
							$("#" + appDivName + " #templateLog").val(logData[appDivName]["templateLog"]);
						}


						// error log
						if (json.data.indexOf("[ERROR START]") > -1){
							logData[appDivName]["errorLog"] = (header + "\n" + logData[appDivName]["errorLog"]).substring(0, maxLogSize);
							$("#" + appDivName + " #errorLog").val(logData[appDivName]["errorLog"]);
						}


						// print custom logs
						if (classe == "AppMonitor"){

							if(!$("#" + appDivName + " #custom #systemMonitor").exists()){
								$("#" + appDivName + " #custom").append("<div id=\"systemMonitor\"></div>");
							}

							if(json.data.indexOf("---------") == -1){
								$("#" + appDivName + " #custom #systemMonitor").html(json.data);
							}


						} else if (classe == "TaskWorker"){

							if(!$("#" + appDivName + " #custom #taskWorker").exists()){
								$("#" + appDivName + " #custom").append("<div id=\"taskWorker\"></div>");
							}

							$("#" + appDivName + " #custom #taskWorker").html(json.data);


						}
					
					}
				}

			}

        };
	
        ws.onerror = function(e) {};
		
        ws.onclose = function() {
			$("title").text("Rebooting...");
			$("h1").text($("title").text());
			$("body").css("background-color", "#FF0000");
			setTimeout(start, startInterval);
		};

	}
	
	start();

});


// get class name
function getClassName(classe){
				
	if(classe.indexOf("$") > -1){
		classe = classe.split("$")[0]
	}
	classes = classe.split(".")
	classe = classes[classes.length - 1]

	// checks if its a method
	if(classe[0].toUpperCase() != classe[0]){
		classe = classes[classes.length - 2]
	}
	
	return classe;
	
}


// errors
function getErrors(){
	
	if(!$("#errors #errors #log").exists()){
		
		$.get("/errors", function(data){
	
			$("#errors #errors").html("");

			for(var i = 0; i < data.length; i++){
			
				var line = "";
			
				for(var j in data[i]){
					line += "  -" + j + ": " + data[i][j];
				}
			
				// log
				var log = $("<div><a id=\"" + data[i]["id"] + "\" href=\"/logs?id=" + data[i]["id"] + "\">" + line + "</a></div>");
			
				log.find("a").click(function(e) {
					e.preventDefault();
					showLog($(this).parent());
				});
			
				// append
				$("#errors #errors").append(log);
			
			}
	
		});
		
	}
	
	setTimeout(getErrors, errorsInterval)
}
getErrors();


// get log
function showLog(div){
	
	if(div.find("#log").exists()){
		div.find("#log").remove();
	
	}else{
		
		$.get("/logs?id=" + div.find("a").attr("id"), function(json){
			
			var table = "<table id=\"log\" border=\"0\" width=\"700px\">"
				+ "<tr>"
				+ "	<td>id</td>"
				+ "	<td>" + json.id + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>publicDNS</td>"
				+ "	<td>" + json.publicDNS + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>pid</td>"
				+ "	<td>" + json.pid + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>logType</td>"
				+ "	<td>" + json.logType + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>methodPath</td>"
				+ "	<td>" + json.methodPath + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>lineNumber</td>"
				+ "	<td>" + json.lineNumber + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>added</td>"
				+ "	<td>" + json.added + "</td>"
				+ "</tr>"
				+ "<tr>"
				+ "	<td>data</td>"
				+ "	<td><textarea style=\"width:800px; height:300px;\">" + json.data + "</textarea></td>"
				+ "</tr>"
				+ "</table>";
				
			div.append(table);
			
		})
		
	}
	
}
	

// pauses the stream
function pause(){
	paused = !paused;
}


// removes the div of a dead app
function removeDeadApp(appDivName){
	
	if(!paused){
		$("#" + appDivName).remove();
	}else{
		setAppRemoveTimeout(appDivName)
	}
	
}


// removes the div of a dead server
function removeDeadServer(serverDivName){
	
	if(!paused){
		$("#" + serverDivName).remove();
	}else{
		setServerRemoveTimeout(serverDivName)
	}
	
}


// kills a process
function kill(pid, publicDNS){
	if(window.confirm("Are you sure you want to kill pid: " + pid + "?")){
		$.get("http://" + publicDNS + ":" + port + "/kill " + pid);
	}
}


// app timeout
function setAppRemoveTimeout(appDivName){
	appsInterval[appDivName] = setTimeout(removeDeadApp, deadInterval, appDivName);
}


// server timeout
function setServerRemoveTimeout(serverDivName){
	serversInterval[serverDivName] = setTimeout(removeDeadServer, deadInterval, serverDivName);
}


// deletes the server
function deleteServer(publicDNS){
	if(window.confirm("Are you sure you want to delete the server: " + publicDNS + "?")){
		$.get("http://" + publicDNS + ":" + port + "/delete");
	}
}


// server stats
function setServerStats(div, json){
	
	json = JSON.parse(json);

	div.html("");
	div.append("<ul>");
	div.append("<li><h3>freeDiskPorcent: " + json.freeDiskPorcent + " | freeMemoryPorcent: " +   json.freeMemoryPorcent + "</h3></li>");
	div.append("<li>uptime: " +                   json.uptime + " secs</li>");
	div.append("<li>freeMemory: " +               json.freeMemory + "</li>");
	div.append("<li>usedMemory: " +               json.usedMemory + "</li>");
	div.append("<li>totalMemory: " +              json.totalMemory + "</li>");
	div.append("<li>cpu: " +                      json.cpu + "</li>");
	div.append("<li>freeDisk: " +                 json.freeDisk + "</li>");
	div.append("<li>usedDisk: " +                 json.usedDisk + "</li>");
	div.append("<li>totalDisk: " +                json.totalDisk + "</li>");
	div.append("</ul>");
	div.append("<br>");
	
}


// checks the databases info
function checkDatabasesInfo(publicDNS, div){
	
	$.get("http://" + publicDNS + ":" + port + "/databases", function(databases){
				
		if(databases.length > 0){
			
			div.html("");
			div.append("<h2>" + publicDNS + "</h2>");
		
			// databases
			for(var i = 0; i < databases.length; i++){
				
				var databaseName = databases[i].name;
				div.append("<h3>" + databaseName + "</h3>");
				
				// tables
				div.append("<ul>");
				for(var a = 0; a < databases[i].tables.length; a++){
					div.append("<li>" + databases[i].tables[a].table + " - " + databases[i].tables[a].size + "</li>");
				}
				
				div.append("</ul>");
			}
			
			div.append("<br>");

		}
		
		setTimeout(checkDatabasesInfo, checkDatabaseInterval, publicDNS, div);
		
	});
	
}