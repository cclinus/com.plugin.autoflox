function func1(bannerID) {
    var prefix = "prefix_";
    ele = document.getElementById(prefix + bannerID); // DDA
    var varInside = 40;   
    ele.setAttribute("width", "100");
    alert('end of func1');
}

function func2(){
    var ele2 = document.getElementById("wrongId");
    varInside = 50;
    ele2.setAttribute("width", "100");
}

var ele;
setTimeout("func2();", 3000);
func1(5);
