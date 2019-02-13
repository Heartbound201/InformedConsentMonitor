

function startTimeMe() {
   TimeMe.initialize({
        currentPageName: "window.location.href"
   });
}

function startWebgazer() {
    webgazer.setTracker("clmtrackr");
    webgazer.setGazeListener(function(data, elapsedTime) {
        if (data == null) {
            return;
        }
        var xprediction = data.x; //these x coordinates are relative to the viewport
        var yprediction = data.y; //these y coordinates are relative to the viewport
        console.log(elapsedTime + ", " + xprediction + ", " + yprediction); //elapsed time is based on time since begin was called
    }).begin();
}

// Paragraphs viewport visibility results obj
var results = {};

 function calculateVisibilityForParagraph(p$) {
    var windowHeight = $(window).height(),
        docScroll = $(document).scrollTop(),
        pPosition = p$.offset().top,
        pHeight = p$.height(),
        hiddenBefore = docScroll - pPosition,
        hiddenAfter = (pPosition + pHeight) - (docScroll + windowHeight);

    if ((docScroll > pPosition + pHeight) || (pPosition > docScroll + windowHeight)) {
        return 0;
    } else {
        var result = 100;

        if (hiddenBefore > 0) {
            result -= (hiddenBefore * 100) / pHeight;
        }

        if (hiddenAfter > 0) {
            result -= (hiddenAfter * 100) / pHeight;
        }

        return result;
    }
}

function calculateAndDisplayForAllParagraphs() {
    $('p').each(function () {
        var p$ = $(this);
        if(p$.attr('id') != null){
            results[p$.attr('id')] = calculateVisibilityForParagraph(p$);
        }
    });

    Native.trackViewportVisibility(JSON.stringify(results));
}

$(document).scroll(function () {
    calculateAndDisplayForAllParagraphs();
});

function androidNativeInterfaceCall() {
   // webgazer.getCurrentPrediction()
   Native.trackData(window.location.href, Math.round(TimeMe.getTimeOnCurrentPageInSeconds()), amountscrolled());
}

console.log("Injection successful.")
startTimeMe();
startWebgazer();
calculateAndDisplayForAllParagraphs();
setInterval(androidNativeInterfaceCall, 1000);

