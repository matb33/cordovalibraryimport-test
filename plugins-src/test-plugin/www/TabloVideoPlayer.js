console.log("[TabloVideoPlayer] installing...");

var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec'),
    cordova = require('cordova');

channel.createSticky('onCordovaInfoReady');
// Tell cordova channel to wait on the CordovaInfoReady event
channel.waitForInitialization('onCordovaInfoReady');


function TabloVideoPlayer() {
	var me = this;

	channel.onCordovaReady.subscribe(function() {
		me.play("http://192.168.1.183/stream/pl.m3u8?A7XuBCmxtBcoImMskvU4hg", function(args) {


		}, function(e) {
			utils.alert("[ERROR] Error playing video Tablo Cordova: " + e);
		});
	});
}

TabloVideoPlayer.prototype.play = function(url, successCallback, errorCallback) {
	exec(successCallback, errorCallback, "TabloVideoPlayer", "play", [url, {}]);
};

module.exports = new TabloVideoPlayer();

console.log("[TabloVideoPlayer] installed.");