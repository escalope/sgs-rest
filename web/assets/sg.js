var source = new EventSource("/sg/events/");

var charts = {};

/* 109 */var CONSUMPTION_SERIES_INDEX = 0;
/* 110 */var GENERATION_SERIES_INDEX = 1;
/* 111 */var DEMAND_SERIES_INDEX = 2;
/* 112 */var LOSSES_SERIES_INDEX = 3;

var dataPoints = [ [], [], [], [] ];

charts.chart1 = new CanvasJS.Chart("chart1", {
	title : {
		text : "SG Simulator data"
	},
	data : [ {
		type : "line",
		dataPoints : dataPoints[0]
	}, {
		type : "line",
		dataPoints : dataPoints[1]
	}, {
		type : "line",
		dataPoints : dataPoints[2]
	}, {
		type : "line",
		dataPoints : dataPoints[3]
	} ]
});

charts.chart1.render();

source.addEventListener('open', function(e) {
	console.log("Connection open");
}, false);

source.addEventListener('system-consumption', function(e) {
	var data = JSON.parse(e.data);
	// sg.data.labels.push(...)
	dataPoints[CONSUMPTION_SERIES_INDEX].push({
		x : new Date(data.timestamp),
		y : data.value
	});
	charts.chart1.render();
}, false);

source.addEventListener('generation-sum', function(e) {
	var data = JSON.parse(e.data);
	if (data.value > 0) {
		dataPoints[GENERATION_SERIES_INDEX].push({
			x : new Date(data.timestamp),
			y : data.value
		});
		charts.chart1.render();
	}
}, false);
source.addEventListener('substation-sensor', function(e) {
	var data = JSON.parse(e.data);
	var dataPoint = {
		x : new Date(data.timestamp),
		y : data.value.value / 1000.0
	};
	if (data.value.sensorId == "POWER_DEMAND") {
		dataPoints[GENERATION_SERIES_INDEX].push(dataPoint);
		charts.chart1.render();
	}
	if (data.value.sensorId == "LOSSES") {
		dataPoints[LOSSES_SERIES_INDEX].push(dataPoint);
		charts.chart1.render();
	}

}, false);

source.addEventListener('error', function(e) {
	if (e.readyState == EventSource.CLOSED) {
		// Connection was closed.
	}
}, false);

// Load sensors

updateTransformers = function(tName) {
	$("#transformer-elements").empty();
	$.getJSON("/sg/query/transformer-elements", {
		"transformer-name" : tName
	}, function(data) {
		for (var i = 0; i < data.result.length; i++) {
			$("#transformer-elements").append($('<button/>', {
				text : data.result[i] + ":",
				id : 'btn_' + i,
				click : function() {
					alert('hi');
				}
			}));
			$("#transformer-elements").append('<br/>');
		}

	});

}

$.getJSON("/sg/query/transformer-names", function(data) {
	var items = [];
	console.log(data);
	$.each(data.result, function(index, tName) {
		$("#transformers")
				.append($("<option></option>").text(tName).val(tName));
	});
	$("#transformers").on('change', function() {
		updateTransformers($("#transformers").val());
	})
});
