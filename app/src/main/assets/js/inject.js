
function trackSession(){
    Native.trackWebSession(new Date().getTime(), window.location.href)
}

function startWebgazer() {

    var gazeDot = document.createElement('div');
    gazeDot.style.position = 'absolute';
    gazeDot.style.left = '20px'; //'-999em';
    gazeDot.style.width = '10px';
    gazeDot.style.height = '10px';
    gazeDot.style.background = 'red';
    gazeDot.style.display = 'none';

    //start the webgazer tracker
    webgazer
        .setRegression('ridge') // currently must set regression and tracker
        .setTracker('clmtrackr')
        .setGazeListener(function(data, clock) {
             //console.log(data); // data is an object containing an x and y key which are the x and y prediction coordinates (no bounds limiting)
             //console.log(clock); // elapsed time in milliseconds since webgazer.begin() was called
        })
        .begin()
        .showPredictionPoints(true) // shows a square every 100 milliseconds where current prediction is
        .showFaceOverlay(false)
        .showVideo(false)
        .showFaceFeedbackBox(false);
}

// Paragraphs visible in viewport and relative visibility percentage
var visible_paragraphs = {};
// Visible paragraphs bounding boxes
var paragraphs_bboxes = {};

function calculateVisibilityForParagraph(par) {
    var windowHeight = $(window).height(),
        docScroll = $(document).scrollTop(),
        pPosition = par.offset().top,
        pHeight = par.height(),
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
    var paragraphs = [];
    $('p.testo').each(function () {
        var par = $(this);
        var paragraph = {};
        visible_paragraphs[par.attr('id')] = calculateVisibilityForParagraph(par);
        paragraphs_bboxes[par.attr('id')] = par[0].getBoundingClientRect();
        paragraph['id'] = par.attr('id');
        paragraph['text'] = par.text();
        paragraph['visibility'] = calculateVisibilityForParagraph(par);
        paragraph['boundingbox'] = par[0].getBoundingClientRect();
        paragraphs.push(paragraph);
    });
    return paragraphs;
}

function predictCurrentParagraph(){
    var filtered_paragraphs = Object.keys(visible_paragraphs).reduce(function (filtered, key) {
        if (visible_paragraphs[key] > 0) filtered[key] = visible_paragraphs[key];
        return filtered;
    }, {});
    var filtered_bboxes = Object.keys(paragraphs_bboxes).reduce(function (filtered, key) {
        if (filtered_paragraphs[key] != null) filtered[key] = paragraphs_bboxes[key];
        return filtered;
    }, {});
    var gaze_position = webgazer.getCurrentPrediction();
    var max = null;
    var predicted_paragraph = Object.keys(filtered_bboxes).forEach(function (key) {
        if(max == null || filtered_paragraphs[key] > filtered_paragraphs[max])
        {
            max = key;
        }
        if( gaze_position != null &&
            gaze_position.x >= filtered_bboxes[key].x &&
            gaze_position.x <= filtered_bboxes[key].x + filtered_bboxes[key].width &&
            gaze_position.y >= filtered_bboxes[key].y &&
            gaze_position.y <= filtered_bboxes[key].y + filtered_bboxes[key].height)
        {
            return key;
        }
    });
    return predicted_paragraph ? predicted_paragraph : max;
}

function androidNativeInterfaceCall() {
   Native.trackJavascriptData(new Date().getTime(), JSON.stringify(calculateAndDisplayForAllParagraphs()), JSON.stringify(getEyePrediction()));
}


$(document).scroll(function () {
    calculateAndDisplayForAllParagraphs();
});

trackSession();
startWebgazer();
calculateAndDisplayForAllParagraphs();
setInterval(androidNativeInterfaceCall, 1000);

function exportWebgazerRegressionData(){
    var data = JSON.stringify(webgazer.getRegression()[0].getData());
    Native.saveWebgazerData(data);
}

function loadWebgazerRegressionData(){
    var data = JSON.parse(Native.loadWebgazerData());
    for (var i = 0; i < data.length; i++) {
        webgazer.getRegression()[0].addData(data[i].eyes, data[i].screenPos, data[i].type);
    }
}

function getEyePrediction(){
    var ret = {};
    var pred = webgazer.getCurrentPrediction();
    if(pred == null){
        return ret;
    }
    ret['x'] = pred.x;
    ret['y'] = pred.y;
    return ret;
}