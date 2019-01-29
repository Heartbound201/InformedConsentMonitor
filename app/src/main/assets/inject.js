

function startTimeMe() {
   TimeMe.initialize();
}

function startWebgazer() {
    webgazer.begin();
}

function androidNativeInterfaceCall() {
   Native.trackTime(TimeMe.getTimeOnCurrentPageInSeconds());
   Native.trackScroll(amountscrolled());
   if(typeof webgazer === 'undefined'){
    Native.trackEyes('Not defined')
   }
   else{
    Native.trackEyes(webgazer.getCurrentPrediction())
   }
}

console.log("Injection successful.")
startTimeMe();
startWebgazer();
setInterval(androidNativeInterfaceCall, 1000);