

function startTimeMe() {
   TimeMe.initialize({
        currentPageName: "window.location.href"
   });
}

function startWebgazer() {
    webgazer.begin();
}

function androidNativeInterfaceCall() {
   //Native.trackTime(TimeMe.getTimeOnCurrentPageInSeconds());
   //Native.trackScroll(amountscrolled());
   //Native.trackEyes(webgazer.getCurrentPrediction())
   Native.trackData(window.location.href, TimeMe.getTimeOnCurrentPageInSeconds(), amountscrolled());
}

console.log("Injection successful.")
startTimeMe();
startWebgazer();
setInterval(androidNativeInterfaceCall, 1000);

