window.onload = initPage;

function initPage() {
	request = createRequest();
	portfolioName = "shortStrat2014Feb";
	if(request==null) {
		alert("Unable to create request");
		return;
	}
	var url = "gvHome?portfolioName=" + portfolioName;
	request.open("GET", url, true);
	request.onreadystatechange=displayDetails;
	request.send(null);
	// get values from server
}

function createRequest() {
	try {
		request = new XMLHttpRequest();
	} catch(failed) {
		request = null;
	}
	return request;
}

function displayDetails() {
	if(request.readyState == 4) {
		if(request.status == 200) {
			var jsonObj = JSON.parse(request.responseText);
			
			detailDev = document.getElemeentById("description");
			detailDiv.innerHTML = request.responseText;
		}
	}
}