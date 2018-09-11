'use strict';

let statsGlobal = undefined;
let lastUpdate = Date.now();
let currentTestType = "";
let browserList = undefined;
let currentlyExpanded = false;

function getElementVisibility(elem) {
    const docViewTop = $(window).scrollTop();
    const docViewBottom = docViewTop + $(window).height();

    const elemTop = $(elem).offset().top;
    const elemBottom = elemTop + $(elem).height();

    return ((elemBottom <= docViewBottom) && (elemTop >= docViewTop));
}

function getStandardRTCStats(stats, scenario) {
    scenario = scenario.replace(/\r?\n|\r|\ /g, '');

    for (const curStat of stats) {
        const browserStats = curStat.browserStats;
        if (scenario === 'Videoonly(P2P)') {
            if (browserStats && browserStats.csioGetStatsDiff && browserStats.csioGetStatsDiff.standardStatsVideoOnly) {
                return browserStats.csioGetStatsDiff.standardStatsVideoOnly;
            }
        } else if (scenario === 'Audioonly(P2P)') {
            if (browserStats && browserStats.csioGetStatsDiff && browserStats.csioGetStatsDiff.standardStatsAudioOnly) {
                return browserStats.csioGetStatsDiff.standardStatsAudioOnly;
            }
        } else if (scenario === 'AudioandVideo(P2P)') {
            if (browserStats && browserStats.csioGetStatsDiff && browserStats.csioGetStatsDiff.standardStatsBoth) {
                return browserStats.csioGetStatsDiff.standardStatsBoth;
            }
        }
    }
    return undefined;
}

function getBrowserList(stats) {
    let browserLists = new Map();
    for (const curStat of stats) {
        const browserDetail = curStat.browser;
        if (browserDetail && browserDetail.name) {
            let browserName = `${browserDetail.name} ${browserDetail.version}`;
            browserName = `${browserName[0].toUpperCase()}${browserName.slice(1)}`;
            browserLists.set(browserName, curStat.browser);
        }
    }
    return browserLists;
}

function getBrowserStatsForScenario(stats, scenario, browserDetails) {
    scenario = scenario.replace(/\r?\n|\r|\ /g, '');

    for (const curStat of stats) {
        if (curStat.browser && curStat.browser.id === browserDetails.id) {
            const curBrowserStats = curStat.browserStats;
            if (scenario === 'Videoonly(P2P)') {
                if (curBrowserStats && curBrowserStats.csioGetStatsDiff && curBrowserStats.csioGetStatsDiff.browserStatsBoth) {
                    return curBrowserStats.csioGetStatsDiff.browserStatsVideoOnly;
                }
            } else if (scenario === 'Audioonly(P2P)') {
                if (curBrowserStats && curBrowserStats.csioGetStatsDiff && curBrowserStats.csioGetStatsDiff.browserStatsAudioOnly) {
                    return curBrowserStats.csioGetStatsDiff.browserStatsAudioOnly;
                }
            } else if (scenario === 'AudioandVideo(P2P)') {
                if (curBrowserStats && curBrowserStats.csioGetStatsDiff && curBrowserStats.csioGetStatsDiff.browserStatsAudioOnly) {
                    return curBrowserStats.csioGetStatsDiff.browserStatsBoth;
                }
            }
        }
    }
    return undefined;
}

function updateBrowserCompatibilityChart(browserDetails, stats, columnNumber) {
    let standardStats = getStandardRTCStats(stats, currentTestType);
    let browserStats = getBrowserStatsForScenario(stats, currentTestType, browserDetails);
    if (!(browserStats && standardStats)) {
        return;
    }
    let totalBrowserScore = 0;
    const statObjectKeys = Object.keys(standardStats);
    //browser-row-end
    for (const curKey of statObjectKeys) {
        const valuesStandard = (standardStats[curKey] || "").replace(/[\[\]\ ]/g, '').split(",");
        const valuesBrowsers = (browserStats[curKey] || "").replace(/[\[\]\ ]/g, '').split(",");

        let currentBrowserScore = 0;
        for (const curVal of valuesStandard) {
            const domElem = `#column-${columnNumber}-${curKey}-${curVal}`;
            if (valuesBrowsers.includes(curVal)) {
                $(domElem).css('background-color', '#47C159');
                currentBrowserScore += 1;
            } else {
                $(domElem).css('background-color', '#757575');
            }
            const scoreDomElem = `.${curKey}-score-column-${columnNumber}`;
            const curStandardScore = $(scoreDomElem).text().split("/").pop();
            $(scoreDomElem).text(`${currentBrowserScore}/${curStandardScore}`)

        }
        totalBrowserScore += currentBrowserScore;

    }
    const domSelector = `#total-score-column-${columnNumber}`;
    const totalScore = $(domSelector).text().split('/').pop();
    $(domSelector).text(`${totalBrowserScore}/${totalScore}`);
}

function onTestTypeSelected(e) {
    const selectedTestType = (e.text || "");
    $('.dropdown-menu-test-type').text(selectedTestType);
    currentTestType = selectedTestType;

    // update standard stats
    addStandardStats(statsGlobal);

    // update browser stats
    for (let i = 1; i <= 4; i += 1) {
        const currentSelectedBrowser = `.column-${i}.selected-browser > a`;
        const browserName = $(currentSelectedBrowser).text();
        if (browserList.has(browserName)) {
            // may be update browser stats
            const browserDetails = browserList.get(browserName);
            console.log('update browser compatibility chart ', browserName, browserDetails);
            updateBrowserCompatibilityChart(browserDetails, statsGlobal, i);
        }
    }
}

// Expand or collapse a particular json key
function expandOrCollapseIndividual(e) {
    const domElement = `.collapse-expand-individual-${e}`;
    const expandCollapseSign = `.oi-plus-minus-${e}`;
    if ($(domElement).css('display') === 'none') {
        $(domElement).css('display', 'block');
        $(expandCollapseSign).removeClass('oi-plus');
        $(expandCollapseSign).addClass('oi-minus');
    } else {
        $(domElement).css('display', 'none');
        $(expandCollapseSign).removeClass('oi-minus');
        $(expandCollapseSign).addClass('oi-plus');
    }
}

// Expand or collapse all
function expandOrCollapseAll(e) {
    const domElement = `.collapse-expand-all`;
    const expandCollapseSign = `.oi-expand-collapse`;
    if (e === 'expand') {
        $(domElement).css('display', 'block');
        $(expandCollapseSign).removeClass('oi-plus');
        $(expandCollapseSign).addClass('oi-minus');
        currentlyExpanded = true;
    } else {
        $(domElement).css('display', 'none');
        $(expandCollapseSign).addClass('oi-plus');
        $(expandCollapseSign).removeClass('oi-minus');
        currentlyExpanded = false;
    }
}

// Add available webRTC standard stats
function addStandardStats(stats) {
    // remove existing standard stats
    $('#rtc-stats-container').empty();

    let standardStats = getStandardRTCStats(stats, currentTestType);
    if(!standardStats){
        return ;
    }

    let totalScore = 0;
    const addStatsValues = (curKey, values) => {
        totalScore += values.length;
        let domElem = "";
        let isEven = true;
        for (const curVal of values) {
            domElem += `<div class="row border" style="background-color: ${isEven? 'rgba(0, 0, 0, 0.05)': 'inherit'};">
                <div class="container-fluid">
                    <div class="row stats-key" style="cursor: pointer">
                        <div class="col-sm-3">
                            <div class="row">
                                <div class="col-sm-3">
                                </div>
                                <div class="col-sm-9 text-left w-100">
                                    <span class="font-weight-normal">${curVal}</span>
                                </div>
                            </div>
                        </div>
                        <div class="col-sm-9">
                                <div class="d-flex justify-content-around bd-highlight">
                                    <div class="bd-highlight w-25">
                                        <div class="mx-auto w-50" id='column-1-${curKey}-${curVal}' style="background-color: #757575">&nbsp;</div>
                                    </div>
                                    <div class="bd-highlight w-25">
                                        <div class="mx-auto w-50" id='column-2-${curKey}-${curVal}' style="background-color: #757575">&nbsp;</div>
                                    </div>
                                    <div class="bd-highlight w-25">
                                        <div class="mx-auto w-50" id='column-3-${curKey}-${curVal}' style="background-color: #757575">&nbsp;</div>
                                    </div>
                                    <div class="bd-highlight w-25">
                                        <div class="mx-auto w-50" id='column-4-${curKey}-${curVal}' style="background-color: #757575">&nbsp;</div>
                                    </div>
                                </div>
                        </div>
                    </div>
                </div>
            </div>`
            isEven ^= true;
        }
        return domElem;
    };
    const addScoreDom = (curKey, totalScore) => {
        const domElem = `
                <div class="d-flex p-2 justify-content-around bd-highlight">
                    <div class="bd-highlight w-25">
                        <div class="text-center w-100 ${curKey}-score-column-1 font-weight-bold"  style="color: #2a2a2a">
                            0/${totalScore}
                        </div>
                    </div>
                    <div class="bd-highlight w-25">
                        <div class="text-center w-100 ${curKey}-score-column-2 font-weight-bold" style="color: #2a2a2a"">
                            0/${totalScore}
                        </div>
                    </div>
                    <div class="bd-highlight w-25">
                        <div class="text-center w-100 ${curKey}-score-column-3 font-weight-bold" style="color: #2a2a2a">
                            0/${totalScore}
                        </div>
                    </div>
                    <div class="bd-highlight w-25">
                        <div class="text-center w-100 ${curKey}-score-column-4 font-weight-bold" style="color: #2a2a2a">
                            0/${totalScore}
                        </div>
                    </div>
                </div>
        `;
        return domElem;
    };
    const addExpandCollapseDom = (curKey, keyAsString) => {
        const domElem = `
                <div class="p-2 bd-highlight">
                    <div class="row show-hide-row">
                        <div class="col-sm-2 text-center w-100 text-light font-weight-light" style="padding-right: 2%">
                            <p onclick='expandOrCollapseIndividual(${keyAsString})' class="font-weight-bold" style="background-color: #9966CC; cursor: pointer; border-radius: 5px;">
                                <span class="oi oi-plus oi-plus-minus-${curKey} oi-expand-collapse">
                            </p> 
                        </div>
                        <div class="col-sm-10 w-100 text-light font-weight-light" style="padding-left: 1%">
                            <p class="font-weight-bold" style="background-color: #9966CC">&nbsp;${curKey}</p>
                        </div>
                    </div>
                </div>
      `;
        return domElem;
    };

    const statObjectKeys = Object.keys(standardStats);
    let itr = 1;
    let domAsString = "";
    for (const curKey of statObjectKeys) {
        const keyAsString = JSON.stringify(curKey);
        const values = (standardStats[curKey] || "").replace(/[\[\]\ ]/g, '').split(",");
        domAsString += `
                <div class='row rtc-content-div div-position-${itr}' style="padding-top: 1%; display: block">
                    <div class="container-fluid">
                        <div class="row">
                            <div class="col-sm-3">
                                ${addExpandCollapseDom(curKey, keyAsString)}
                            </div>
                            <div class="col-sm-9">
                                ${addScoreDom(curKey, values.length)}
                            </div>
                        </div>
                        <div class="collapse-expand-all collapse-expand-individual-${curKey}" style="display: none">
                            ${addStatsValues(curKey, values)}
                        </div>
                        
                    </div>
                </div>`;
        itr += 1;
    }
    $('#rtc-stats-container').append(`${domAsString}`);
    for (let i = 1; i <= 4; i += 1) {
        const domElem = `#total-score-column-${i}`;
        $(domElem).text(`0/${totalScore}`);
    }
    if(currentlyExpanded) {
        // Radio button select expand all
        $("#expand-all-radio-button").click();
    }
}

// UI related updates
function onBrowserSelectionChange(e, browserDetails) {
    const getColumnNumber = (className) => {
        for (let i = 1; i <= 4; i += 1) {
            if (className.includes(`column-${i}`)) {
                return i;
            }
        }
        return -1;
    };

    const changeOptionName = (parentDom, browserName) => {
        $(parentDom).find('.browser-version').text(browserName);
    };
    const changeBrowserIcon = (columnNumber, details) => {
        if (columnNumber !== -1) {
            const logoDom = `.browser-logo.column-${columnNumber} > img`;
            const imgUrl = `assets/img/${details.name}.png`;
            $(logoDom).attr('src', imgUrl);
        }
    };
    if (e && e.parent() && e.parent().parent()) {
        const parentDom = e.parent().parent();
        if (parentDom) {
            const browserName = e.text();
            const className = parentDom.attr("class");
            const columnNumber = getColumnNumber(className);
            if (columnNumber !== -1) {
                changeOptionName(parentDom, browserName);
                changeBrowserIcon(columnNumber, browserDetails);
                updateBrowserCompatibilityChart(browserDetails, statsGlobal, columnNumber);
            }
        }
    }
}

function updateBrowserOptions(stats) {
    browserList = getBrowserList(stats);
    for (const [key, browserDetail] of browserList) {
        const value = JSON.stringify(browserDetail);
        const browserName = key;
        const htmlDom = `<a style="font-size: 14px; cursor: pointer" class='dropdown-item ${browserName}' id='${browserName}' onclick='onBrowserSelectionChange($(this), ${value})'>${browserName}</a>`;
        $('.browser-option').append(htmlDom);
    }
}
function showSubscribeModal() {
    $("#subscribeModal").modal('show');
}

function addDefault() {
    /*$("#validation-page").parent().addClass("disabled");
    $("#verification-page").parent().removeClass("disabled");*/

    // Select audi only as default
    const audioOnlyOption = $("#scenario-selecor .dropdown-item")[2];
    if(  audioOnlyOption ) {
        $(audioOnlyOption).click();
    }

    const getFirstOfKind = (browserOption, browserName) => {
        for(const curDom of browserOption) {
            const className = curDom.className || "";
            if( className.includes(browserName) ){
                return curDom;
            }
        }
        if( browserOption && browserOption.length > 0 ){
            return browserOption[0];
        }
        return undefined;
    };
    // Select default browser for second column
    const defaultBrowserOneOption = getFirstOfKind( $(".browser-option.browser-1 > .dropdown-item"), 'Firefox' );
    if( defaultBrowserOneOption) {
        $(defaultBrowserOneOption).click();
    }

    const defaultBrowserTwoOption = getFirstOfKind( $(".browser-option.browser-2 > .dropdown-item"), 'Chrome' );
    if( defaultBrowserTwoOption) {
        $(defaultBrowserTwoOption).click();
    }

    // Radio button select expand all
    $("#expand-all-radio-button").click()
}

function showStats(stats) {
    statsGlobal = stats;
    updateBrowserOptions(stats);
    addDefault();
}

function showStatsFor(statsFor) {
    console.log('stats for', statsFor);
    if (statsFor === 'verification') {
        $('.validation-page').css('display', 'none');
        $('.verification-page').css('display', 'block');

        $("#validation-page").addClass("font-weight-light");
        $("#verification-page").removeClass("font-weight-light");

        $("#validation-page").parent().addClass("disabled");
        $("#verification-page").parent().removeClass("disabled");
    } else if (statsFor === 'validation') {
        $('.verification-page').css('display', 'none');
        $('.validation-page').css('display', 'block');

        $("#validation-page").removeClass("font-weight-light");
        $("#verification-page").addClass("font-weight-light");

        $("#validation-page").parent().removeClass("disabled");
        $("#verification-page").parent().addClass("disabled");
    }
}

$(document).ready(function () {
    (() => {
        if(requestURL.includes("rtcstatsjson")) {
            $.ajax({
                url: requestURL,
                success: function (result) {
                    showStats(JSON.parse(result));
                }
            });
        }
    })();
});

function adjustView() {
    if (Date.now() - lastUpdate > 1000) {
        lastUpdate = Date.now();
        const browserDom = $('#browser-row');
        const retval = getElementVisibility(browserDom);
        if (!retval) {
            $('html, body').animate({
                scrollTop: $("#browser-row").offset().top
            }, 50);
        }
    }
}
$(document).scroll(function () {
    adjustView();
});
$("#rtc-stats-container").scroll(function () {
    adjustView();
});
