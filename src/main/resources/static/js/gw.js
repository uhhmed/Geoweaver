/**
* 
* date: 20180925
* 
*/

edu = {
		gmu: {
			csiss: {
				geoweaver:{

					desc: "a web system to allow users to easily compose and execute full-stack " +
							"deep learning workflows in web browsers " +
							"by taking advantage of the distributed online spatial data facilities, high-performance " +
							"computation platforms, and open-source deep learning libraries. ",
							
					sponsor: "ESIPLab incubator project, NASA ACCESS project, NSF Geoinformatics project",
					
					version: "0.9.7",
					
					author: "open source contributors",
					
					institute: "NASA, NSF, George Mason University, ESIP, University of Texas Austin, University of Washington, University of Idaho, "
					
				}
			}
		}
};

// GW will be the short name of the package
var GW = edu.gmu.csiss.geoweaver

//put all the shared added function here as global depenedency
String.prototype.replaceAll = function(search, replacement) {
	var target = this;
	return target.replace(new RegExp(search, 'g'), replacement);
};