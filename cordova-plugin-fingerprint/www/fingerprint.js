module.exports = {

  auth(success, error) {
    cordova.exec(success, error, 'FingerprintPlugin', 'auth', null)
  }

}
