# cordova-plugin-mpc

This Cordova plugin provides JavaScript API, that allows you to communicate with MPD (https://www.musicpd.org) server. Currently we support Android platform only.

## Installation

```
cordova plugin add https://github.com/seatwork/cordova-plugin-mpc
```

## Simple Usage

Connect MPD server with window.mpc: 
```
mpc.connect('10.0.0.2', 6600, res => {
  console.log('connect success')
}, err => {
  console.error('connect failed: ', err)
})
```

Send command message to MPD server:
```
mpc.command('send message', res => {
  console.log('receive message: ', res)
})
```

Disconnect MPD server:
```
mpc.disconnect(res => {
  console.log('disconnect success')
}, err => {
  console.error('disconnect failed: ', err)
})
```

Move app to background:
```
mpc.moveToBackground()
```

## MPD Commands

["add","addid","addtagid","channels","clear","clearerror","cleartagid","close","commands","config","consume","count","crossfade","currentsong","decoders","delete","deleteid","disableoutput","enableoutput","find","findadd","idle","kill","list","listall","listallinfo","listfiles","listmounts","listplaylist","listplaylistinfo","listplaylists","load","lsinfo","mixrampdb","mixrampdelay","mount","move","moveid","next","notcommands","outputs","password","pause","ping","play","playid","playlist","playlistadd","playlistclear","playlistdelete","playlistfind","playlistid","playlistinfo","playlistmove","playlistsearch","plchanges","plchangesposid","previous","prio","prioid","random","rangeid","readcomments","readmessages","rename","repeat","replay_gain_mode","replay_gain_status","rescan","rm","save","search","searchadd","searchaddpl","seek","seekcur","seekid","sendmessage","setvol","shuffle","single","stats","status","stop","subscribe","swap","swapid","tagtypes","toggleoutput","unmount","unsubscribe","update","urlhandlers","volume"]

- References: https://www.musicpd.org
