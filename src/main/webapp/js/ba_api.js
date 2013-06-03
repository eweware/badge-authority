var spinner = null;
var spinner_options = {
  lines: 9, // The number of lines to draw
  length: 20, // The length of each line
  width: 10, // The line thickness
  radius: 30, // The radius of the inner circle
  corners: 1, // Corner roundness (0..1)
  rotate: 0, // The rotation offset
  direction: 1, // 1: clockwise, -1: counterclockwise
  color: '#000', // #rgb or #rrggbb
  speed: 1, // Rounds per second
  trail: 60, // Afterglow percentage
  shadow: false, // Whether to render a shadow
  hwaccel: false, // Whether to use hardware acceleration
  className: 'spinner', // The CSS class to assign to the spinner
  zIndex: 2e9, // The z-index (defaults to 2000000000)
  top: 'auto', // Top position relative to parent in px
  left: 'auto' // Left position relative to parent in px
};
var ba_email_address = null;
var ba_verification_code = null;

function ba_submit1() {
  ba_start_spinner();
  var ev = ba_email_address;
  var tkv = document.getElementById('ba_tk').value;
  var endpoint = document.getElementById('ba_end').value;
  var query = '?tk='+tkv+'&e='+ev;
  ba_rest('POST', endpoint + '/badges/credentials'+query, null, ba_okf, ba_errf);
}

function ba_submit2() {
  ba_start_spinner();
  var code = ba_verification_code;
  var tkv = document.getElementById('ba_tk').value;
  var endpoint = document.getElementById('ba_end').value;
  var query = '?tk='+tkv+'&c='+ba_verification_code;
  ba_rest('POST', endpoint + '/badges/verify'+query, null, ba_okf, ba_errf);
}

function ba_submit3() {
  ba_start_spinner();
  var e = document.getElementById('ba_e').value;
  var d = document.getElementById('ba_d').value;
  var endpoint = document.getElementById('ba_end').value;
  var query = '?e='+e+"&d="+d;
  ba_rest('POST', endpoint + '/badges/support'+query, null, ba_okf, ba_errf);
}

function ba_cancel_submit(code) {
    if (ba_dialog_closed) {
         ba_dialog_closed(code);
    } else {
       $("#badgedialog").empty();
       throw "missing ba_dialog_closed";
    }
}

function ba_okf(result, a, b) {
  ba_stop_spinner();
  $("#badgedialog").html(result);
}

function ba_errf(a, b, c) {
   ba_stop_spinner();
   alert('ERROR! STATUS='+a.status+' ('+a.statusText+'): '+a.responseText);
}

function ba_start_spinner() {
  var el = document.getElementById('ba_form');
  if (el) {
      if (spinner == null) {
          spinner =  new Spinner(spinner_options).spin(el);
      } else {
          spinner.spin(el)
      }
  }
}

function ba_stop_spinner() {
   if (spinner) {
     spinner.stop();
   }
}

function ba_rest(method, path, entity, okFunction, errorFunction) {
$.ajax({
		type : method,
		url : path,
		data : entity,
		contentType : "application/json; charset=utf-8",
		dataType : "html",
		success : okFunction,
		error : errorFunction
	});
}

