var ba_email_address = null;
var ba_verification_code = null;

function ba_submit1() {
  var ev = ba_email_address;
  var tkv = document.getElementById('ba_tk').value;
  var endpoint = document.getElementById('ba_end').value;
  var query = '?tk='+tkv+'&e='+ev;
  ba_rest('POST', endpoint + '/badges/credentials'+query, null, ba_okf, ba_errf);
}

function ba_submit2() {
  var code = ba_verification_code;
  var tkv = document.getElementById('ba_tk').value;
  var endpoint = document.getElementById('ba_end').value;
  var query = '?tk='+tkv+'&c='+ba_verification_code;
  ba_rest('POST', endpoint + '/badges/verify'+query, null, ba_okf, ba_errf);
}

function ba_submit3() {
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
  $("#badgedialog").html(result);   // TODO use replace on ba_form instead of this
}

function ba_errf(a, b, c) {
    alert('ERROR! STATUS='+a.status+' ('+a.statusText+'): '+a.responseText);
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

