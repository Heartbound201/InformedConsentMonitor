

function startTimeMe() {
   TimeMe.initialize();
}

function androidNativeInterfaceCall() {
   Native.trackTime(TimeMe.getTimeOnCurrentPageInSeconds());
   Native.trackScroll(amountscrolled());
}

console.log("Injection successful.")
startTimeMe();

setInterval(androidNativeInterfaceCall, 1000);