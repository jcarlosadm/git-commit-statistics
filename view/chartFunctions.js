function createChart(id, data) {
var disciplined = [], undisciplined = [], loc = [], toAppend = false, bubble = [], showZoom = false;
for (i = 0; i < data.y.length; i++) {		
	if (i > 0 && parseInt(data.y[i][1]) > parseInt(data.y[i - 1][1]) && parseInt(data.y[i][0]) < parseInt(data.y[i - 1][0])) {
		showZoom = data.x.length > 50 ? true : false
		bubble.push({x:i, y:parseInt(data.y[i][1]) > parseInt(data.y[i][0])? parseInt(data.y[i][1]): parseInt(data.y[i][0]), z: 1, d:parseInt(data.y[i][1]), u:parseInt(data.y[i][0])});		
		disciplined.push({
		y: parseInt(data.y[i][1]),
                marker: {
                    symbol: 'url(http://easy-rbsm.rhcloud.com/exclamation.jpg)'
                }
			});
		undisciplined.push({
		y: parseInt(data.y[i][0]),
                marker: {
                    symbol: 'url(http://easy-rbsm.rhcloud.com/exclamation.jpg)'
                }
			});
	} else {
		disciplined.push(parseInt(data.y[i][1]));
		undisciplined.push(parseInt(data.y[i][0]));
	}
	
	loc.push(parseInt(data.y[i][2]));
	if(parseInt(data.y[i][1]) !== 0 || parseInt(data.y[i][0]) !== 0) {
		toAppend = true;
	}
}
if (toAppend) {
	$('#' + id).highcharts({
		chart: {
                zoomType: 'x'
        },
        title: {
            text: 'Code Analysis',
            x: -20 //center
        },
        subtitle: {
            text: 'Source: ' + id.substring(id.indexOf('-') + 1),
            x: -20
        },
        xAxis: {
	title: {
                text: 'Commits'
            },
            categories: data.x
        },
        yAxis: {
            title: {
                text: 'Number'
            },
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
		plotOptions: {
                series: {
                    cursor: 'pointer',
                    point: {
                        events: {
                            click: function (e) {
                                hs.htmlExpand(null, {
                                    pageOrigin: {
                                        x: e.pageX || e.clientX,
                                        y: e.pageY || e.clientY
                                    },
                                    headingText: this.series.name,
                                    maincontentText: '<br><b>Number:</b> ' + this.y + '<br><br><b>Link:</b><a href="' + gitLink + this.category +'" target="_blank">' + this.category,
                                    width: 370
                                });
                            }
                        }
                    },
                    marker: {
                        lineWidth: 1
                    }
                }
            },
        legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'middle',
            borderWidth: 0
        },
        series: [{
            name: 'Disciplined',
            data: disciplined
        }, {
            name: 'Undisciplined',
            data: undisciplined
        }, {
            name: 'Number of lines',
            data: loc,
			visible: false
        },
		{
            name: 'Zoom alert',
            data: bubble,
			type: 'bubble',
			visible: showZoom,
			tooltip: { 
			headerFormat: '',
			pointFormat: 'Disciplined: {point.d}<br>Undisciplined: {point.u}<br/>' 
			},
			marker: {
                lineColor: 'transparent',
                fillColor: 'transparent',
				states: {
					select: {
						lineColor: 'transparent',
						fillColor: 'transparent',
						enabled: false
					},
					hover: {
						lineColor: 'transparent',
						fillColor: 'transparent',
						enabled: false
					}
				}
				
            },
			dataLabels: {
                enabled: true,
                format: 'Zoom',
				verticalAlign: 'middle',
				y: -20
            },
			
        }]
    });

	} else {
		$('#' + id).remove();
	}
}
$(function () {
	for (var key in info) {
	  if (info.hasOwnProperty(key)) {
		$('#container').append('<div id="chart-' + key + '" style="min-width: 310px; height: 400px; margin: 0 auto"></div>');
		createChart('chart-' + key, info[key]);
	  }
	}
    
});