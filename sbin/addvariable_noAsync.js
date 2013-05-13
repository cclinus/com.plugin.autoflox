window.xhr = new XMLHttpRequest();
window.buffer = new Array();
var totalValue;
function send(value) {
	window.buffer.push(value);
	totalValue += value;
	if (totalValue.indexOf(':::ERROR') !== -1) {
		totalValue = "";
		sendReally();
	}
}
function sendReally() {
	window.xhr.open('POST', 'receiveData.php', false);
	window.xhr.send(JSON.stringify(window.buffer));
	window.buffer = new Array();
}
function addVariable(name, value) {
	if (typeof (value) == 'object') {
		if (value instanceof Array) {
			if (value.length > 0) {
				return new Array(name, typeof (value[0]) + '_array', value);
			} else {
				return new Array(name, 'object_array', value);
			}
		}
	} else if (typeof (value) != 'undefined' && typeof (value) != 'function') {
		return new Array(name, typeof (value), value);
	}
	return new Array(name, typeof (value), 'undefined');
};