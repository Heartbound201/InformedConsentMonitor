

function startTimeMe() {
   TimeMe.initialize({
        currentPageName: "window.location.href"
   });
}

function startWebgazer() {
    webgazer.begin();
}

function androidNativeInterfaceCall() {
   // webgazer.getCurrentPrediction()
   Native.trackData(window.location.href, Math.round(TimeMe.getTimeOnCurrentPageInSeconds()), amountscrolled());
}

console.log("Injection successful.")
startTimeMe();
startWebgazer();
setInterval(androidNativeInterfaceCall, 1000);

