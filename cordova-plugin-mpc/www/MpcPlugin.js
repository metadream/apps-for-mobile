var exec = require('cordova/exec')
module.exports = {

  connect(host, port, success, error) {
    exec(success, error, 'MpcPlugin', 'connect', [ host, port ])
  },

  disconnect(success, error) {
    exec(success, error, 'MpcPlugin', 'disconnect', [])
  },

  command(arg, success, error) {
    exec(success, error, 'MpcPlugin', 'command', [ arg ])
  },

  moveToBackground(success, error) {
    exec(success, error, 'MpcPlugin', 'moveToBackground', [])
  }

}
