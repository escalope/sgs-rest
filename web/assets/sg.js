var source = new EventSource("/sg/events/");
var charts = {};
var TIME_WINDOW = 10;
/* 109 */var CONSUMPTION_SERIES_INDEX = 0;
/* 110 */var GENERATION_SERIES_INDEX = 1;
/* 111 */var DEMAND_SERIES_INDEX = 2;
/* 112 */var LOSSES_SERIES_INDEX = 3;

/* 113 */var SUN = 4;
/* 114 */var WIND = 5;
/* 115 */var BILL = 6;
/* 116 */var LASTDEMAND = 7;
/* 117 */var LASTEXPORT = 8;
/* 118 */var LASTIMPORT = 9;
/* 119 */var LASTGENERATED = 10;
/* 120 */var LASTCONSUMPTION = 11;
/* 121 */var LASTPOWERDUMP = 12;



var dataPoints = [[], [], [], [], [],[],[],[],[],[],[],[],[]];

charts.chart1 = new CanvasJS.Chart("chart1", {
    title: {
        text: "Power Metering at Substation"
    },
     legend: {
       horizontalAlign: "right", // "center" , "right"
       verticalAlign: "top",  // "top" , "bottom"
       fontSize: 12
     },axisX:{      
				
				title:"Hour of the day",
				labelAngle: -50,
				labelFontColor: "rgb(0,75,141)"

			},
	
			axisY: {
				title: "kW",
				interlacedColor: "azure",
				tickColor: "azure",
				titleFontColor: "rgb(0,75,141)",
                                interval: 5
				
				
			},
height: 400,
    data: [

{
            type: "line",showInLegend: true,
      legendText: "Consumption",markerType:"circle",markerSize:15,
            dataPoints: dataPoints[LASTCONSUMPTION]
        }, 
 {
            type: "line",showInLegend: true,markerType:"triangle",markerSize:15,
      legendText: "Generated",
            dataPoints: dataPoints[LASTGENERATED]
        },
{
            type: "line",showInLegend: true,markerType:"triangle",markerSize:15,
      legendText: "Demand",
            dataPoints: dataPoints[LASTDEMAND]
        }
    ]
});

charts.chart2 = new CanvasJS.Chart("chart2", {
    title: {
        text: "Weather"
    },
height: 400,
     legend: {
      horizontalAlign: "right", // "center" , "right"
       verticalAlign: "top",  // "top" , "bottom"
       fontSize: 12
     },
axisX:{      
				
				title:"Hour of the day",
				labelAngle: -50,
				labelFontColor: "rgb(0,75,141)",				
			},
	
			axisY: {
				title: "% of activation",
				interlacedColor: "azure",
				tickColor: "azure",
				titleFontColor: "rgb(0,75,141)",
                                interval: 10
				
				
			},
    data: [{
            type: "line",showInLegend: true,markerType:"triangle",markerSize:15,
      legendText: "% of Sun over optimum",
            dataPoints: dataPoints[4]
        }, {
            type: "line",showInLegend: true,markerType:"circle",markerSize:15,
      legendText: "% of Wind over optimum",
            dataPoints: dataPoints[5]
        }]
});

charts.chart3 = new CanvasJS.Chart("chart3", {
    title: {
        text: "BILLING"
    },
height: 400,
     legend: {
      horizontalAlign: "right", // "center" , "right"
       verticalAlign: "top",  // "top" , "bottom"
       fontSize: 12
     },
    data: [{
            type: "line",showInLegend: true,
      legendText: "Bill",
            dataPoints: dataPoints[BILL]
        }]
});


charts.chart4 = new CanvasJS.Chart("chart4", {
    title: {
        text: "Total Power Demand From Substation"
    },
height: 400,
     legend: {
      horizontalAlign: "right", // "center" , "right"
       verticalAlign: "top",  // "top" , "bottom"
       fontSize: 12
     },
    data: [{
            type: "line",showInLegend: true,
      legendText: "Power demand (negative is bad)",
            dataPoints: dataPoints[LASTPOWERDUMP]
        }]
});



source.addEventListener('open', function (e) {
    console.log("Connection open");
}, false);

source.addEventListener('error', function (e) {
    if (e.readyState == EventSource.CLOSED) {
        // Connection was closed.
    }
}, false);
// Load sensors

updateListeners = function(eventname,chart,index){
    source.addEventListener(eventname, function (e) {
    var data = JSON.parse(e.data);
    //if (data.value > 0) {
        dataPoints[index].push({
            x: new Date(data.timestamp),
            y: data.value
        });
        if (dataPoints[index].length > TIME_WINDOW)
            dataPoints[index].shift();
        chart.render();
    //}
}, false);
}

updateCounterListener = function(eventname,divid){
    source.addEventListener(eventname, function (e) {
    var data = JSON.parse(e.data);
    $(divid).empty(); 
    $(divid).append(data.value);
}, false);
}

 


deactivatePanel=function(buttonText,indice,maxp){
return function () {
                    requestdata={"deviceName":buttonText};
                    $.ajax({url: "/sg/execute/switch-off",
                        type: "POST",
                        "async": true,
                         processData:false,
                        data: JSON.stringify(requestdata),
                         dataType:"json" , 
                          "cache-control" : "no-cache",
                        contentType: "application/json; charset=utf-8",      
                        complete: function(jsondata){$("#btn_"+indice).text(texto+":last order was off");}}).done(function (response) {
$("#btn_"+indice).click(activatePanel(buttonText,indice,maxp));
   
});
                }
}

activatePanel=function(buttonText,indice,maxp){
return function () {
                    requestdata={"deviceName":buttonText};
                    $.ajax({url: "/sg/execute/switch-on",
                        type: "POST",
                        "async": true,
                         processData:false,
                        data: JSON.stringify(requestdata),
                         dataType:"json" , 
                          "cache-control" : "no-cache",
                        contentType: "application/json; charset=utf-8",      
                        complete: function(jsondata){$("#btn_"+indice).text(texto+":last order was on");}}).done(function (response) {
$("#btn_"+indice).click(deactivatePanel(buttonText,indice,maxp));
   
});
                }
}

generatePanel=function(tName,panelName, index,maxp){
            texto=panelName;
            indice=index;
            $("#"+tName).append($('<button/>', {
                text: texto + ":on",
                id: 'btn_' + indice,
                click: deactivatePanel(texto, indice,maxp)
            }));
            $("#"+tName).append(' Max power '+maxp+' kW<br/>');
}

panelIndexes=0;
var pname="";
updateTransformers = function (tName) {
    //$("#transformer-elements").empty();    
    
    $.getJSON("/sg/query/transformer-generators", {
        "transformer-name": tName
    }, function (data) {
        $("#transformer-elements").append("<div id=\""+tName+"\"><h2>"+tName+"</h2></div>");
        for (var i = 0, pname=""; i < data.result.length; i++) {
            {pname=data.result[i];            
            $.ajax({type:"GET",
                    datatype:"json",
                    async: false,
                    url:"/sg/query/solarpanel-power", 
                    data: {"solar-panel":pname},
                    success: function(power){
                    
                    generatePanel(tName,pname,panelIndexes,Math.abs(power.result)/1000);   
                    panelIndexes=panelIndexes+1;           
                    console.log(pname);
                    console.log(panelIndexes);
            }});            
            }   
           
        }
    });
}

