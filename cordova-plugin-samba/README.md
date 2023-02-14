# cordova-plugin-samba

## Installation

```
cordova plugin add https://github.com/seatwork/cordova-plugin-samba
```

## Simple Usage

Sets username and password authentication:
```
samba.auth('username', 'password')
```

Lists files and directories by path. The path must be ends with '/', ex. smb://10.0.0.2/sharefolder/directory/
```
samba.listEntries(path, success, error)
```

Reads content by path:
```
samba.readAsText(path, success, error)
samba.readAsByteArray(path, success, error)
```

Uploads local file to smb server:
```
samba.upload(localPath, smbPath, success, error)
samba.onProgress = function(progress) {
    console.log(progress)
}
```

Downloads remote file to local storage:
```
samba.download(smbPath, success, error)
samba.onProgress = function(progress) {
    console.log(progress)
}
```

Creates empty directory or file:
```
samba.createDirectory(path, success, error)
samba.createFile(path, success, error)
```

Deletes file or directory:
```
samba.delete(path, success, error)
```

Open image:
```
samba.openImage(path, success, error)
```

Open media (video or audio):
```
samba.openMedia(path, success, error)
```

Open file with native app:
```
samba.openFile(path, success, error)
```

Wake the server on lan by MAC address:
```
samba.wakeOnLan(mac, port, success, error)
```
