const exec = require('cordova/exec')
module.exports = {

  runBackground(success, error) {
    exec(success, error, 'SambaPlugin', 'runBackground', null)
  },

  auth(username, password, success, error) {
    exec(success, error, 'SambaPlugin', 'auth', [ username, password ])
  },

  listEntries(path, success, error) {
    exec(success, error, 'SambaPlugin', 'listEntries', [ path ])
  },

  readAsText(path, success, error) {
    exec(success, error, 'SambaPlugin', 'readAsText', [ path ])
  },

  readAsByteArray(path, success, error) {
    exec(success, error, 'SambaPlugin', 'readAsByteArray', [ path ])
  },

  openImage(path, success, error) {
    exec(success, error, 'SambaPlugin', 'openImage', [ path ])
  },

  openMedia(path, success, error) {
    exec(success, error, 'SambaPlugin', 'openMedia', [ path ])
  },

  openFile(path, success, error) {
    exec(success, error, 'SambaPlugin', 'openFile', [ path ])
  },

  upload(localPath, smbPath, success, error) {
    exec(success, error, 'SambaPlugin', 'upload', [ localPath, smbPath ])
  },

  download(smbPath, success, error) {
    exec(success, error, 'SambaPlugin', 'download', [ smbPath ])
  },

  createFile(path, success, error) {
    exec(success, error, 'SambaPlugin', 'createFile', [ path ])
  },

  createDirectory(path, success, error) {
    exec(success, error, 'SambaPlugin', 'createDirectory', [ path ])
  },

  delete(path, success, error) {
    exec(success, error, 'SambaPlugin', 'delete', [ path ])
  },

  wakeOnLan(mac, port, success, error) {
    exec(success, error, 'SambaPlugin', 'wakeOnLan', [ mac, port ])
  }

}
