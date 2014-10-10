(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
/*!
 * The buffer module from node.js, for the browser.
 *
 * @author   Feross Aboukhadijeh <feross@feross.org> <http://feross.org>
 * @license  MIT
 */

var base64 = require('base64-js')
var ieee754 = require('ieee754')
var isArray = require('is-array')

exports.Buffer = Buffer
exports.SlowBuffer = Buffer
exports.INSPECT_MAX_BYTES = 50
Buffer.poolSize = 8192 // not used by this implementation

var kMaxLength = 0x3fffffff

/**
 * If `Buffer.TYPED_ARRAY_SUPPORT`:
 *   === true    Use Uint8Array implementation (fastest)
 *   === false   Use Object implementation (most compatible, even IE6)
 *
 * Browsers that support typed arrays are IE 10+, Firefox 4+, Chrome 7+, Safari 5.1+,
 * Opera 11.6+, iOS 4.2+.
 *
 * Note:
 *
 * - Implementation must support adding new properties to `Uint8Array` instances.
 *   Firefox 4-29 lacked support, fixed in Firefox 30+.
 *   See: https://bugzilla.mozilla.org/show_bug.cgi?id=695438.
 *
 *  - Chrome 9-10 is missing the `TypedArray.prototype.subarray` function.
 *
 *  - IE10 has a broken `TypedArray.prototype.subarray` function which returns arrays of
 *    incorrect length in some situations.
 *
 * We detect these buggy browsers and set `Buffer.TYPED_ARRAY_SUPPORT` to `false` so they will
 * get the Object implementation, which is slower but will work correctly.
 */
Buffer.TYPED_ARRAY_SUPPORT = (function () {
  try {
    var buf = new ArrayBuffer(0)
    var arr = new Uint8Array(buf)
    arr.foo = function () { return 42 }
    return 42 === arr.foo() && // typed array instances can be augmented
        typeof arr.subarray === 'function' && // chrome 9-10 lack `subarray`
        new Uint8Array(1).subarray(1, 1).byteLength === 0 // ie10 has broken `subarray`
  } catch (e) {
    return false
  }
})()

/**
 * Class: Buffer
 * =============
 *
 * The Buffer constructor returns instances of `Uint8Array` that are augmented
 * with function properties for all the node `Buffer` API functions. We use
 * `Uint8Array` so that square bracket notation works as expected -- it returns
 * a single octet.
 *
 * By augmenting the instances, we can avoid modifying the `Uint8Array`
 * prototype.
 */
function Buffer (subject, encoding, noZero) {
  if (!(this instanceof Buffer))
    return new Buffer(subject, encoding, noZero)

  var type = typeof subject

  // Find the length
  var length
  if (type === 'number')
    length = subject > 0 ? subject >>> 0 : 0
  else if (type === 'string') {
    if (encoding === 'base64')
      subject = base64clean(subject)
    length = Buffer.byteLength(subject, encoding)
  } else if (type === 'object' && subject !== null) { // assume object is array-like
    if (subject.type === 'Buffer' && isArray(subject.data))
      subject = subject.data
    length = +subject.length > 0 ? Math.floor(+subject.length) : 0
  } else
    throw new TypeError('must start with number, buffer, array or string')

  if (this.length > kMaxLength)
    throw new RangeError('Attempt to allocate Buffer larger than maximum ' +
      'size: 0x' + kMaxLength.toString(16) + ' bytes')

  var buf
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    // Preferred: Return an augmented `Uint8Array` instance for best performance
    buf = Buffer._augment(new Uint8Array(length))
  } else {
    // Fallback: Return THIS instance of Buffer (created by `new`)
    buf = this
    buf.length = length
    buf._isBuffer = true
  }

  var i
  if (Buffer.TYPED_ARRAY_SUPPORT && typeof subject.byteLength === 'number') {
    // Speed optimization -- use set if we're copying from a typed array
    buf._set(subject)
  } else if (isArrayish(subject)) {
    // Treat array-ish objects as a byte array
    if (Buffer.isBuffer(subject)) {
      for (i = 0; i < length; i++)
        buf[i] = subject.readUInt8(i)
    } else {
      for (i = 0; i < length; i++)
        buf[i] = ((subject[i] % 256) + 256) % 256
    }
  } else if (type === 'string') {
    buf.write(subject, 0, encoding)
  } else if (type === 'number' && !Buffer.TYPED_ARRAY_SUPPORT && !noZero) {
    for (i = 0; i < length; i++) {
      buf[i] = 0
    }
  }

  return buf
}

Buffer.isBuffer = function (b) {
  return !!(b != null && b._isBuffer)
}

Buffer.compare = function (a, b) {
  if (!Buffer.isBuffer(a) || !Buffer.isBuffer(b))
    throw new TypeError('Arguments must be Buffers')

  var x = a.length
  var y = b.length
  for (var i = 0, len = Math.min(x, y); i < len && a[i] === b[i]; i++) {}
  if (i !== len) {
    x = a[i]
    y = b[i]
  }
  if (x < y) return -1
  if (y < x) return 1
  return 0
}

Buffer.isEncoding = function (encoding) {
  switch (String(encoding).toLowerCase()) {
    case 'hex':
    case 'utf8':
    case 'utf-8':
    case 'ascii':
    case 'binary':
    case 'base64':
    case 'raw':
    case 'ucs2':
    case 'ucs-2':
    case 'utf16le':
    case 'utf-16le':
      return true
    default:
      return false
  }
}

Buffer.concat = function (list, totalLength) {
  if (!isArray(list)) throw new TypeError('Usage: Buffer.concat(list[, length])')

  if (list.length === 0) {
    return new Buffer(0)
  } else if (list.length === 1) {
    return list[0]
  }

  var i
  if (totalLength === undefined) {
    totalLength = 0
    for (i = 0; i < list.length; i++) {
      totalLength += list[i].length
    }
  }

  var buf = new Buffer(totalLength)
  var pos = 0
  for (i = 0; i < list.length; i++) {
    var item = list[i]
    item.copy(buf, pos)
    pos += item.length
  }
  return buf
}

Buffer.byteLength = function (str, encoding) {
  var ret
  str = str + ''
  switch (encoding || 'utf8') {
    case 'ascii':
    case 'binary':
    case 'raw':
      ret = str.length
      break
    case 'ucs2':
    case 'ucs-2':
    case 'utf16le':
    case 'utf-16le':
      ret = str.length * 2
      break
    case 'hex':
      ret = str.length >>> 1
      break
    case 'utf8':
    case 'utf-8':
      ret = utf8ToBytes(str).length
      break
    case 'base64':
      ret = base64ToBytes(str).length
      break
    default:
      ret = str.length
  }
  return ret
}

// pre-set for values that may exist in the future
Buffer.prototype.length = undefined
Buffer.prototype.parent = undefined

// toString(encoding, start=0, end=buffer.length)
Buffer.prototype.toString = function (encoding, start, end) {
  var loweredCase = false

  start = start >>> 0
  end = end === undefined || end === Infinity ? this.length : end >>> 0

  if (!encoding) encoding = 'utf8'
  if (start < 0) start = 0
  if (end > this.length) end = this.length
  if (end <= start) return ''

  while (true) {
    switch (encoding) {
      case 'hex':
        return hexSlice(this, start, end)

      case 'utf8':
      case 'utf-8':
        return utf8Slice(this, start, end)

      case 'ascii':
        return asciiSlice(this, start, end)

      case 'binary':
        return binarySlice(this, start, end)

      case 'base64':
        return base64Slice(this, start, end)

      case 'ucs2':
      case 'ucs-2':
      case 'utf16le':
      case 'utf-16le':
        return utf16leSlice(this, start, end)

      default:
        if (loweredCase)
          throw new TypeError('Unknown encoding: ' + encoding)
        encoding = (encoding + '').toLowerCase()
        loweredCase = true
    }
  }
}

Buffer.prototype.equals = function (b) {
  if(!Buffer.isBuffer(b)) throw new TypeError('Argument must be a Buffer')
  return Buffer.compare(this, b) === 0
}

Buffer.prototype.inspect = function () {
  var str = ''
  var max = exports.INSPECT_MAX_BYTES
  if (this.length > 0) {
    str = this.toString('hex', 0, max).match(/.{2}/g).join(' ')
    if (this.length > max)
      str += ' ... '
  }
  return '<Buffer ' + str + '>'
}

Buffer.prototype.compare = function (b) {
  if (!Buffer.isBuffer(b)) throw new TypeError('Argument must be a Buffer')
  return Buffer.compare(this, b)
}

// `get` will be removed in Node 0.13+
Buffer.prototype.get = function (offset) {
  console.log('.get() is deprecated. Access using array indexes instead.')
  return this.readUInt8(offset)
}

// `set` will be removed in Node 0.13+
Buffer.prototype.set = function (v, offset) {
  console.log('.set() is deprecated. Access using array indexes instead.')
  return this.writeUInt8(v, offset)
}

function hexWrite (buf, string, offset, length) {
  offset = Number(offset) || 0
  var remaining = buf.length - offset
  if (!length) {
    length = remaining
  } else {
    length = Number(length)
    if (length > remaining) {
      length = remaining
    }
  }

  // must be an even number of digits
  var strLen = string.length
  if (strLen % 2 !== 0) throw new Error('Invalid hex string')

  if (length > strLen / 2) {
    length = strLen / 2
  }
  for (var i = 0; i < length; i++) {
    var byte = parseInt(string.substr(i * 2, 2), 16)
    if (isNaN(byte)) throw new Error('Invalid hex string')
    buf[offset + i] = byte
  }
  return i
}

function utf8Write (buf, string, offset, length) {
  var charsWritten = blitBuffer(utf8ToBytes(string), buf, offset, length)
  return charsWritten
}

function asciiWrite (buf, string, offset, length) {
  var charsWritten = blitBuffer(asciiToBytes(string), buf, offset, length)
  return charsWritten
}

function binaryWrite (buf, string, offset, length) {
  return asciiWrite(buf, string, offset, length)
}

function base64Write (buf, string, offset, length) {
  var charsWritten = blitBuffer(base64ToBytes(string), buf, offset, length)
  return charsWritten
}

function utf16leWrite (buf, string, offset, length) {
  var charsWritten = blitBuffer(utf16leToBytes(string), buf, offset, length)
  return charsWritten
}

Buffer.prototype.write = function (string, offset, length, encoding) {
  // Support both (string, offset, length, encoding)
  // and the legacy (string, encoding, offset, length)
  if (isFinite(offset)) {
    if (!isFinite(length)) {
      encoding = length
      length = undefined
    }
  } else {  // legacy
    var swap = encoding
    encoding = offset
    offset = length
    length = swap
  }

  offset = Number(offset) || 0
  var remaining = this.length - offset
  if (!length) {
    length = remaining
  } else {
    length = Number(length)
    if (length > remaining) {
      length = remaining
    }
  }
  encoding = String(encoding || 'utf8').toLowerCase()

  var ret
  switch (encoding) {
    case 'hex':
      ret = hexWrite(this, string, offset, length)
      break
    case 'utf8':
    case 'utf-8':
      ret = utf8Write(this, string, offset, length)
      break
    case 'ascii':
      ret = asciiWrite(this, string, offset, length)
      break
    case 'binary':
      ret = binaryWrite(this, string, offset, length)
      break
    case 'base64':
      ret = base64Write(this, string, offset, length)
      break
    case 'ucs2':
    case 'ucs-2':
    case 'utf16le':
    case 'utf-16le':
      ret = utf16leWrite(this, string, offset, length)
      break
    default:
      throw new TypeError('Unknown encoding: ' + encoding)
  }
  return ret
}

Buffer.prototype.toJSON = function () {
  return {
    type: 'Buffer',
    data: Array.prototype.slice.call(this._arr || this, 0)
  }
}

function base64Slice (buf, start, end) {
  if (start === 0 && end === buf.length) {
    return base64.fromByteArray(buf)
  } else {
    return base64.fromByteArray(buf.slice(start, end))
  }
}

function utf8Slice (buf, start, end) {
  var res = ''
  var tmp = ''
  end = Math.min(buf.length, end)

  for (var i = start; i < end; i++) {
    if (buf[i] <= 0x7F) {
      res += decodeUtf8Char(tmp) + String.fromCharCode(buf[i])
      tmp = ''
    } else {
      tmp += '%' + buf[i].toString(16)
    }
  }

  return res + decodeUtf8Char(tmp)
}

function asciiSlice (buf, start, end) {
  var ret = ''
  end = Math.min(buf.length, end)

  for (var i = start; i < end; i++) {
    ret += String.fromCharCode(buf[i])
  }
  return ret
}

function binarySlice (buf, start, end) {
  return asciiSlice(buf, start, end)
}

function hexSlice (buf, start, end) {
  var len = buf.length

  if (!start || start < 0) start = 0
  if (!end || end < 0 || end > len) end = len

  var out = ''
  for (var i = start; i < end; i++) {
    out += toHex(buf[i])
  }
  return out
}

function utf16leSlice (buf, start, end) {
  var bytes = buf.slice(start, end)
  var res = ''
  for (var i = 0; i < bytes.length; i += 2) {
    res += String.fromCharCode(bytes[i] + bytes[i + 1] * 256)
  }
  return res
}

Buffer.prototype.slice = function (start, end) {
  var len = this.length
  start = ~~start
  end = end === undefined ? len : ~~end

  if (start < 0) {
    start += len;
    if (start < 0)
      start = 0
  } else if (start > len) {
    start = len
  }

  if (end < 0) {
    end += len
    if (end < 0)
      end = 0
  } else if (end > len) {
    end = len
  }

  if (end < start)
    end = start

  if (Buffer.TYPED_ARRAY_SUPPORT) {
    return Buffer._augment(this.subarray(start, end))
  } else {
    var sliceLen = end - start
    var newBuf = new Buffer(sliceLen, undefined, true)
    for (var i = 0; i < sliceLen; i++) {
      newBuf[i] = this[i + start]
    }
    return newBuf
  }
}

/*
 * Need to make sure that buffer isn't trying to write out of bounds.
 */
function checkOffset (offset, ext, length) {
  if ((offset % 1) !== 0 || offset < 0)
    throw new RangeError('offset is not uint')
  if (offset + ext > length)
    throw new RangeError('Trying to access beyond buffer length')
}

Buffer.prototype.readUInt8 = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 1, this.length)
  return this[offset]
}

Buffer.prototype.readUInt16LE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 2, this.length)
  return this[offset] | (this[offset + 1] << 8)
}

Buffer.prototype.readUInt16BE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 2, this.length)
  return (this[offset] << 8) | this[offset + 1]
}

Buffer.prototype.readUInt32LE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)

  return ((this[offset]) |
      (this[offset + 1] << 8) |
      (this[offset + 2] << 16)) +
      (this[offset + 3] * 0x1000000)
}

Buffer.prototype.readUInt32BE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)

  return (this[offset] * 0x1000000) +
      ((this[offset + 1] << 16) |
      (this[offset + 2] << 8) |
      this[offset + 3])
}

Buffer.prototype.readInt8 = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 1, this.length)
  if (!(this[offset] & 0x80))
    return (this[offset])
  return ((0xff - this[offset] + 1) * -1)
}

Buffer.prototype.readInt16LE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 2, this.length)
  var val = this[offset] | (this[offset + 1] << 8)
  return (val & 0x8000) ? val | 0xFFFF0000 : val
}

Buffer.prototype.readInt16BE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 2, this.length)
  var val = this[offset + 1] | (this[offset] << 8)
  return (val & 0x8000) ? val | 0xFFFF0000 : val
}

Buffer.prototype.readInt32LE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)

  return (this[offset]) |
      (this[offset + 1] << 8) |
      (this[offset + 2] << 16) |
      (this[offset + 3] << 24)
}

Buffer.prototype.readInt32BE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)

  return (this[offset] << 24) |
      (this[offset + 1] << 16) |
      (this[offset + 2] << 8) |
      (this[offset + 3])
}

Buffer.prototype.readFloatLE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)
  return ieee754.read(this, offset, true, 23, 4)
}

Buffer.prototype.readFloatBE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 4, this.length)
  return ieee754.read(this, offset, false, 23, 4)
}

Buffer.prototype.readDoubleLE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 8, this.length)
  return ieee754.read(this, offset, true, 52, 8)
}

Buffer.prototype.readDoubleBE = function (offset, noAssert) {
  if (!noAssert)
    checkOffset(offset, 8, this.length)
  return ieee754.read(this, offset, false, 52, 8)
}

function checkInt (buf, value, offset, ext, max, min) {
  if (!Buffer.isBuffer(buf)) throw new TypeError('buffer must be a Buffer instance')
  if (value > max || value < min) throw new TypeError('value is out of bounds')
  if (offset + ext > buf.length) throw new TypeError('index out of range')
}

Buffer.prototype.writeUInt8 = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 1, 0xff, 0)
  if (!Buffer.TYPED_ARRAY_SUPPORT) value = Math.floor(value)
  this[offset] = value
  return offset + 1
}

function objectWriteUInt16 (buf, value, offset, littleEndian) {
  if (value < 0) value = 0xffff + value + 1
  for (var i = 0, j = Math.min(buf.length - offset, 2); i < j; i++) {
    buf[offset + i] = (value & (0xff << (8 * (littleEndian ? i : 1 - i)))) >>>
      (littleEndian ? i : 1 - i) * 8
  }
}

Buffer.prototype.writeUInt16LE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 2, 0xffff, 0)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = value
    this[offset + 1] = (value >>> 8)
  } else objectWriteUInt16(this, value, offset, true)
  return offset + 2
}

Buffer.prototype.writeUInt16BE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 2, 0xffff, 0)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = (value >>> 8)
    this[offset + 1] = value
  } else objectWriteUInt16(this, value, offset, false)
  return offset + 2
}

function objectWriteUInt32 (buf, value, offset, littleEndian) {
  if (value < 0) value = 0xffffffff + value + 1
  for (var i = 0, j = Math.min(buf.length - offset, 4); i < j; i++) {
    buf[offset + i] = (value >>> (littleEndian ? i : 3 - i) * 8) & 0xff
  }
}

Buffer.prototype.writeUInt32LE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 4, 0xffffffff, 0)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset + 3] = (value >>> 24)
    this[offset + 2] = (value >>> 16)
    this[offset + 1] = (value >>> 8)
    this[offset] = value
  } else objectWriteUInt32(this, value, offset, true)
  return offset + 4
}

Buffer.prototype.writeUInt32BE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 4, 0xffffffff, 0)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = (value >>> 24)
    this[offset + 1] = (value >>> 16)
    this[offset + 2] = (value >>> 8)
    this[offset + 3] = value
  } else objectWriteUInt32(this, value, offset, false)
  return offset + 4
}

Buffer.prototype.writeInt8 = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 1, 0x7f, -0x80)
  if (!Buffer.TYPED_ARRAY_SUPPORT) value = Math.floor(value)
  if (value < 0) value = 0xff + value + 1
  this[offset] = value
  return offset + 1
}

Buffer.prototype.writeInt16LE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 2, 0x7fff, -0x8000)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = value
    this[offset + 1] = (value >>> 8)
  } else objectWriteUInt16(this, value, offset, true)
  return offset + 2
}

Buffer.prototype.writeInt16BE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 2, 0x7fff, -0x8000)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = (value >>> 8)
    this[offset + 1] = value
  } else objectWriteUInt16(this, value, offset, false)
  return offset + 2
}

Buffer.prototype.writeInt32LE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = value
    this[offset + 1] = (value >>> 8)
    this[offset + 2] = (value >>> 16)
    this[offset + 3] = (value >>> 24)
  } else objectWriteUInt32(this, value, offset, true)
  return offset + 4
}

Buffer.prototype.writeInt32BE = function (value, offset, noAssert) {
  value = +value
  offset = offset >>> 0
  if (!noAssert)
    checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
  if (value < 0) value = 0xffffffff + value + 1
  if (Buffer.TYPED_ARRAY_SUPPORT) {
    this[offset] = (value >>> 24)
    this[offset + 1] = (value >>> 16)
    this[offset + 2] = (value >>> 8)
    this[offset + 3] = value
  } else objectWriteUInt32(this, value, offset, false)
  return offset + 4
}

function checkIEEE754 (buf, value, offset, ext, max, min) {
  if (value > max || value < min) throw new TypeError('value is out of bounds')
  if (offset + ext > buf.length) throw new TypeError('index out of range')
}

function writeFloat (buf, value, offset, littleEndian, noAssert) {
  if (!noAssert)
    checkIEEE754(buf, value, offset, 4, 3.4028234663852886e+38, -3.4028234663852886e+38)
  ieee754.write(buf, value, offset, littleEndian, 23, 4)
  return offset + 4
}

Buffer.prototype.writeFloatLE = function (value, offset, noAssert) {
  return writeFloat(this, value, offset, true, noAssert)
}

Buffer.prototype.writeFloatBE = function (value, offset, noAssert) {
  return writeFloat(this, value, offset, false, noAssert)
}

function writeDouble (buf, value, offset, littleEndian, noAssert) {
  if (!noAssert)
    checkIEEE754(buf, value, offset, 8, 1.7976931348623157E+308, -1.7976931348623157E+308)
  ieee754.write(buf, value, offset, littleEndian, 52, 8)
  return offset + 8
}

Buffer.prototype.writeDoubleLE = function (value, offset, noAssert) {
  return writeDouble(this, value, offset, true, noAssert)
}

Buffer.prototype.writeDoubleBE = function (value, offset, noAssert) {
  return writeDouble(this, value, offset, false, noAssert)
}

// copy(targetBuffer, targetStart=0, sourceStart=0, sourceEnd=buffer.length)
Buffer.prototype.copy = function (target, target_start, start, end) {
  var source = this

  if (!start) start = 0
  if (!end && end !== 0) end = this.length
  if (!target_start) target_start = 0

  // Copy 0 bytes; we're done
  if (end === start) return
  if (target.length === 0 || source.length === 0) return

  // Fatal error conditions
  if (end < start) throw new TypeError('sourceEnd < sourceStart')
  if (target_start < 0 || target_start >= target.length)
    throw new TypeError('targetStart out of bounds')
  if (start < 0 || start >= source.length) throw new TypeError('sourceStart out of bounds')
  if (end < 0 || end > source.length) throw new TypeError('sourceEnd out of bounds')

  // Are we oob?
  if (end > this.length)
    end = this.length
  if (target.length - target_start < end - start)
    end = target.length - target_start + start

  var len = end - start

  if (len < 100 || !Buffer.TYPED_ARRAY_SUPPORT) {
    for (var i = 0; i < len; i++) {
      target[i + target_start] = this[i + start]
    }
  } else {
    target._set(this.subarray(start, start + len), target_start)
  }
}

// fill(value, start=0, end=buffer.length)
Buffer.prototype.fill = function (value, start, end) {
  if (!value) value = 0
  if (!start) start = 0
  if (!end) end = this.length

  if (end < start) throw new TypeError('end < start')

  // Fill 0 bytes; we're done
  if (end === start) return
  if (this.length === 0) return

  if (start < 0 || start >= this.length) throw new TypeError('start out of bounds')
  if (end < 0 || end > this.length) throw new TypeError('end out of bounds')

  var i
  if (typeof value === 'number') {
    for (i = start; i < end; i++) {
      this[i] = value
    }
  } else {
    var bytes = utf8ToBytes(value.toString())
    var len = bytes.length
    for (i = start; i < end; i++) {
      this[i] = bytes[i % len]
    }
  }

  return this
}

/**
 * Creates a new `ArrayBuffer` with the *copied* memory of the buffer instance.
 * Added in Node 0.12. Only available in browsers that support ArrayBuffer.
 */
Buffer.prototype.toArrayBuffer = function () {
  if (typeof Uint8Array !== 'undefined') {
    if (Buffer.TYPED_ARRAY_SUPPORT) {
      return (new Buffer(this)).buffer
    } else {
      var buf = new Uint8Array(this.length)
      for (var i = 0, len = buf.length; i < len; i += 1) {
        buf[i] = this[i]
      }
      return buf.buffer
    }
  } else {
    throw new TypeError('Buffer.toArrayBuffer not supported in this browser')
  }
}

// HELPER FUNCTIONS
// ================

var BP = Buffer.prototype

/**
 * Augment a Uint8Array *instance* (not the Uint8Array class!) with Buffer methods
 */
Buffer._augment = function (arr) {
  arr._isBuffer = true

  // save reference to original Uint8Array get/set methods before overwriting
  arr._get = arr.get
  arr._set = arr.set

  // deprecated, will be removed in node 0.13+
  arr.get = BP.get
  arr.set = BP.set

  arr.write = BP.write
  arr.toString = BP.toString
  arr.toLocaleString = BP.toString
  arr.toJSON = BP.toJSON
  arr.equals = BP.equals
  arr.compare = BP.compare
  arr.copy = BP.copy
  arr.slice = BP.slice
  arr.readUInt8 = BP.readUInt8
  arr.readUInt16LE = BP.readUInt16LE
  arr.readUInt16BE = BP.readUInt16BE
  arr.readUInt32LE = BP.readUInt32LE
  arr.readUInt32BE = BP.readUInt32BE
  arr.readInt8 = BP.readInt8
  arr.readInt16LE = BP.readInt16LE
  arr.readInt16BE = BP.readInt16BE
  arr.readInt32LE = BP.readInt32LE
  arr.readInt32BE = BP.readInt32BE
  arr.readFloatLE = BP.readFloatLE
  arr.readFloatBE = BP.readFloatBE
  arr.readDoubleLE = BP.readDoubleLE
  arr.readDoubleBE = BP.readDoubleBE
  arr.writeUInt8 = BP.writeUInt8
  arr.writeUInt16LE = BP.writeUInt16LE
  arr.writeUInt16BE = BP.writeUInt16BE
  arr.writeUInt32LE = BP.writeUInt32LE
  arr.writeUInt32BE = BP.writeUInt32BE
  arr.writeInt8 = BP.writeInt8
  arr.writeInt16LE = BP.writeInt16LE
  arr.writeInt16BE = BP.writeInt16BE
  arr.writeInt32LE = BP.writeInt32LE
  arr.writeInt32BE = BP.writeInt32BE
  arr.writeFloatLE = BP.writeFloatLE
  arr.writeFloatBE = BP.writeFloatBE
  arr.writeDoubleLE = BP.writeDoubleLE
  arr.writeDoubleBE = BP.writeDoubleBE
  arr.fill = BP.fill
  arr.inspect = BP.inspect
  arr.toArrayBuffer = BP.toArrayBuffer

  return arr
}

var INVALID_BASE64_RE = /[^+\/0-9A-z]/g

function base64clean (str) {
  // Node strips out invalid characters like \n and \t from the string, base64-js does not
  str = stringtrim(str).replace(INVALID_BASE64_RE, '')
  // Node allows for non-padded base64 strings (missing trailing ===), base64-js does not
  while (str.length % 4 !== 0) {
    str = str + '='
  }
  return str
}

function stringtrim (str) {
  if (str.trim) return str.trim()
  return str.replace(/^\s+|\s+$/g, '')
}

function isArrayish (subject) {
  return isArray(subject) || Buffer.isBuffer(subject) ||
      subject && typeof subject === 'object' &&
      typeof subject.length === 'number'
}

function toHex (n) {
  if (n < 16) return '0' + n.toString(16)
  return n.toString(16)
}

function utf8ToBytes (str) {
  var byteArray = []
  for (var i = 0; i < str.length; i++) {
    var b = str.charCodeAt(i)
    if (b <= 0x7F) {
      byteArray.push(b)
    } else {
      var start = i
      if (b >= 0xD800 && b <= 0xDFFF) i++
      var h = encodeURIComponent(str.slice(start, i+1)).substr(1).split('%')
      for (var j = 0; j < h.length; j++) {
        byteArray.push(parseInt(h[j], 16))
      }
    }
  }
  return byteArray
}

function asciiToBytes (str) {
  var byteArray = []
  for (var i = 0; i < str.length; i++) {
    // Node's code seems to be doing this and not & 0x7F..
    byteArray.push(str.charCodeAt(i) & 0xFF)
  }
  return byteArray
}

function utf16leToBytes (str) {
  var c, hi, lo
  var byteArray = []
  for (var i = 0; i < str.length; i++) {
    c = str.charCodeAt(i)
    hi = c >> 8
    lo = c % 256
    byteArray.push(lo)
    byteArray.push(hi)
  }

  return byteArray
}

function base64ToBytes (str) {
  return base64.toByteArray(str)
}

function blitBuffer (src, dst, offset, length) {
  for (var i = 0; i < length; i++) {
    if ((i + offset >= dst.length) || (i >= src.length))
      break
    dst[i + offset] = src[i]
  }
  return i
}

function decodeUtf8Char (str) {
  try {
    return decodeURIComponent(str)
  } catch (err) {
    return String.fromCharCode(0xFFFD) // UTF 8 invalid char
  }
}

},{"base64-js":2,"ieee754":3,"is-array":4}],2:[function(require,module,exports){
var lookup = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

;(function (exports) {
	'use strict';

  var Arr = (typeof Uint8Array !== 'undefined')
    ? Uint8Array
    : Array

	var PLUS   = '+'.charCodeAt(0)
	var SLASH  = '/'.charCodeAt(0)
	var NUMBER = '0'.charCodeAt(0)
	var LOWER  = 'a'.charCodeAt(0)
	var UPPER  = 'A'.charCodeAt(0)

	function decode (elt) {
		var code = elt.charCodeAt(0)
		if (code === PLUS)
			return 62 // '+'
		if (code === SLASH)
			return 63 // '/'
		if (code < NUMBER)
			return -1 //no match
		if (code < NUMBER + 10)
			return code - NUMBER + 26 + 26
		if (code < UPPER + 26)
			return code - UPPER
		if (code < LOWER + 26)
			return code - LOWER + 26
	}

	function b64ToByteArray (b64) {
		var i, j, l, tmp, placeHolders, arr

		if (b64.length % 4 > 0) {
			throw new Error('Invalid string. Length must be a multiple of 4')
		}

		// the number of equal signs (place holders)
		// if there are two placeholders, than the two characters before it
		// represent one byte
		// if there is only one, then the three characters before it represent 2 bytes
		// this is just a cheap hack to not do indexOf twice
		var len = b64.length
		placeHolders = '=' === b64.charAt(len - 2) ? 2 : '=' === b64.charAt(len - 1) ? 1 : 0

		// base64 is 4/3 + up to two characters of the original data
		arr = new Arr(b64.length * 3 / 4 - placeHolders)

		// if there are placeholders, only get up to the last complete 4 chars
		l = placeHolders > 0 ? b64.length - 4 : b64.length

		var L = 0

		function push (v) {
			arr[L++] = v
		}

		for (i = 0, j = 0; i < l; i += 4, j += 3) {
			tmp = (decode(b64.charAt(i)) << 18) | (decode(b64.charAt(i + 1)) << 12) | (decode(b64.charAt(i + 2)) << 6) | decode(b64.charAt(i + 3))
			push((tmp & 0xFF0000) >> 16)
			push((tmp & 0xFF00) >> 8)
			push(tmp & 0xFF)
		}

		if (placeHolders === 2) {
			tmp = (decode(b64.charAt(i)) << 2) | (decode(b64.charAt(i + 1)) >> 4)
			push(tmp & 0xFF)
		} else if (placeHolders === 1) {
			tmp = (decode(b64.charAt(i)) << 10) | (decode(b64.charAt(i + 1)) << 4) | (decode(b64.charAt(i + 2)) >> 2)
			push((tmp >> 8) & 0xFF)
			push(tmp & 0xFF)
		}

		return arr
	}

	function uint8ToBase64 (uint8) {
		var i,
			extraBytes = uint8.length % 3, // if we have 1 byte left, pad 2 bytes
			output = "",
			temp, length

		function encode (num) {
			return lookup.charAt(num)
		}

		function tripletToBase64 (num) {
			return encode(num >> 18 & 0x3F) + encode(num >> 12 & 0x3F) + encode(num >> 6 & 0x3F) + encode(num & 0x3F)
		}

		// go through the array every three bytes, we'll deal with trailing stuff later
		for (i = 0, length = uint8.length - extraBytes; i < length; i += 3) {
			temp = (uint8[i] << 16) + (uint8[i + 1] << 8) + (uint8[i + 2])
			output += tripletToBase64(temp)
		}

		// pad the end with zeros, but make sure to not forget the extra bytes
		switch (extraBytes) {
			case 1:
				temp = uint8[uint8.length - 1]
				output += encode(temp >> 2)
				output += encode((temp << 4) & 0x3F)
				output += '=='
				break
			case 2:
				temp = (uint8[uint8.length - 2] << 8) + (uint8[uint8.length - 1])
				output += encode(temp >> 10)
				output += encode((temp >> 4) & 0x3F)
				output += encode((temp << 2) & 0x3F)
				output += '='
				break
		}

		return output
	}

	exports.toByteArray = b64ToByteArray
	exports.fromByteArray = uint8ToBase64
}(typeof exports === 'undefined' ? (this.base64js = {}) : exports))

},{}],3:[function(require,module,exports){
exports.read = function(buffer, offset, isLE, mLen, nBytes) {
  var e, m,
      eLen = nBytes * 8 - mLen - 1,
      eMax = (1 << eLen) - 1,
      eBias = eMax >> 1,
      nBits = -7,
      i = isLE ? (nBytes - 1) : 0,
      d = isLE ? -1 : 1,
      s = buffer[offset + i];

  i += d;

  e = s & ((1 << (-nBits)) - 1);
  s >>= (-nBits);
  nBits += eLen;
  for (; nBits > 0; e = e * 256 + buffer[offset + i], i += d, nBits -= 8);

  m = e & ((1 << (-nBits)) - 1);
  e >>= (-nBits);
  nBits += mLen;
  for (; nBits > 0; m = m * 256 + buffer[offset + i], i += d, nBits -= 8);

  if (e === 0) {
    e = 1 - eBias;
  } else if (e === eMax) {
    return m ? NaN : ((s ? -1 : 1) * Infinity);
  } else {
    m = m + Math.pow(2, mLen);
    e = e - eBias;
  }
  return (s ? -1 : 1) * m * Math.pow(2, e - mLen);
};

exports.write = function(buffer, value, offset, isLE, mLen, nBytes) {
  var e, m, c,
      eLen = nBytes * 8 - mLen - 1,
      eMax = (1 << eLen) - 1,
      eBias = eMax >> 1,
      rt = (mLen === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0),
      i = isLE ? 0 : (nBytes - 1),
      d = isLE ? 1 : -1,
      s = value < 0 || (value === 0 && 1 / value < 0) ? 1 : 0;

  value = Math.abs(value);

  if (isNaN(value) || value === Infinity) {
    m = isNaN(value) ? 1 : 0;
    e = eMax;
  } else {
    e = Math.floor(Math.log(value) / Math.LN2);
    if (value * (c = Math.pow(2, -e)) < 1) {
      e--;
      c *= 2;
    }
    if (e + eBias >= 1) {
      value += rt / c;
    } else {
      value += rt * Math.pow(2, 1 - eBias);
    }
    if (value * c >= 2) {
      e++;
      c /= 2;
    }

    if (e + eBias >= eMax) {
      m = 0;
      e = eMax;
    } else if (e + eBias >= 1) {
      m = (value * c - 1) * Math.pow(2, mLen);
      e = e + eBias;
    } else {
      m = value * Math.pow(2, eBias - 1) * Math.pow(2, mLen);
      e = 0;
    }
  }

  for (; mLen >= 8; buffer[offset + i] = m & 0xff, i += d, m /= 256, mLen -= 8);

  e = (e << mLen) | m;
  eLen += mLen;
  for (; eLen > 0; buffer[offset + i] = e & 0xff, i += d, e /= 256, eLen -= 8);

  buffer[offset + i - d] |= s * 128;
};

},{}],4:[function(require,module,exports){

/**
 * isArray
 */

var isArray = Array.isArray;

/**
 * toString
 */

var str = Object.prototype.toString;

/**
 * Whether or not the given `val`
 * is an array.
 *
 * example:
 *
 *        isArray([]);
 *        // > true
 *        isArray(arguments);
 *        // > false
 *        isArray('');
 *        // > false
 *
 * @param {mixed} val
 * @return {bool}
 */

module.exports = isArray || function (val) {
  return !! val && '[object Array]' == str.call(val);
};

},{}],5:[function(require,module,exports){
/*!
 * jQuery JavaScript Library v2.1.1
 * http://jquery.com/
 *
 * Includes Sizzle.js
 * http://sizzlejs.com/
 *
 * Copyright 2005, 2014 jQuery Foundation, Inc. and other contributors
 * Released under the MIT license
 * http://jquery.org/license
 *
 * Date: 2014-05-01T17:11Z
 */

(function( global, factory ) {

	if ( typeof module === "object" && typeof module.exports === "object" ) {
		// For CommonJS and CommonJS-like environments where a proper window is present,
		// execute the factory and get jQuery
		// For environments that do not inherently posses a window with a document
		// (such as Node.js), expose a jQuery-making factory as module.exports
		// This accentuates the need for the creation of a real window
		// e.g. var jQuery = require("jquery")(window);
		// See ticket #14549 for more info
		module.exports = global.document ?
			factory( global, true ) :
			function( w ) {
				if ( !w.document ) {
					throw new Error( "jQuery requires a window with a document" );
				}
				return factory( w );
			};
	} else {
		factory( global );
	}

// Pass this if window is not defined yet
}(typeof window !== "undefined" ? window : this, function( window, noGlobal ) {

// Can't do this because several apps including ASP.NET trace
// the stack via arguments.caller.callee and Firefox dies if
// you try to trace through "use strict" call chains. (#13335)
// Support: Firefox 18+
//

var arr = [];

var slice = arr.slice;

var concat = arr.concat;

var push = arr.push;

var indexOf = arr.indexOf;

var class2type = {};

var toString = class2type.toString;

var hasOwn = class2type.hasOwnProperty;

var support = {};



var
	// Use the correct document accordingly with window argument (sandbox)
	document = window.document,

	version = "2.1.1",

	// Define a local copy of jQuery
	jQuery = function( selector, context ) {
		// The jQuery object is actually just the init constructor 'enhanced'
		// Need init if jQuery is called (just allow error to be thrown if not included)
		return new jQuery.fn.init( selector, context );
	},

	// Support: Android<4.1
	// Make sure we trim BOM and NBSP
	rtrim = /^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g,

	// Matches dashed string for camelizing
	rmsPrefix = /^-ms-/,
	rdashAlpha = /-([\da-z])/gi,

	// Used by jQuery.camelCase as callback to replace()
	fcamelCase = function( all, letter ) {
		return letter.toUpperCase();
	};

jQuery.fn = jQuery.prototype = {
	// The current version of jQuery being used
	jquery: version,

	constructor: jQuery,

	// Start with an empty selector
	selector: "",

	// The default length of a jQuery object is 0
	length: 0,

	toArray: function() {
		return slice.call( this );
	},

	// Get the Nth element in the matched element set OR
	// Get the whole matched element set as a clean array
	get: function( num ) {
		return num != null ?

			// Return just the one element from the set
			( num < 0 ? this[ num + this.length ] : this[ num ] ) :

			// Return all the elements in a clean array
			slice.call( this );
	},

	// Take an array of elements and push it onto the stack
	// (returning the new matched element set)
	pushStack: function( elems ) {

		// Build a new jQuery matched element set
		var ret = jQuery.merge( this.constructor(), elems );

		// Add the old object onto the stack (as a reference)
		ret.prevObject = this;
		ret.context = this.context;

		// Return the newly-formed element set
		return ret;
	},

	// Execute a callback for every element in the matched set.
	// (You can seed the arguments with an array of args, but this is
	// only used internally.)
	each: function( callback, args ) {
		return jQuery.each( this, callback, args );
	},

	map: function( callback ) {
		return this.pushStack( jQuery.map(this, function( elem, i ) {
			return callback.call( elem, i, elem );
		}));
	},

	slice: function() {
		return this.pushStack( slice.apply( this, arguments ) );
	},

	first: function() {
		return this.eq( 0 );
	},

	last: function() {
		return this.eq( -1 );
	},

	eq: function( i ) {
		var len = this.length,
			j = +i + ( i < 0 ? len : 0 );
		return this.pushStack( j >= 0 && j < len ? [ this[j] ] : [] );
	},

	end: function() {
		return this.prevObject || this.constructor(null);
	},

	// For internal use only.
	// Behaves like an Array's method, not like a jQuery method.
	push: push,
	sort: arr.sort,
	splice: arr.splice
};

jQuery.extend = jQuery.fn.extend = function() {
	var options, name, src, copy, copyIsArray, clone,
		target = arguments[0] || {},
		i = 1,
		length = arguments.length,
		deep = false;

	// Handle a deep copy situation
	if ( typeof target === "boolean" ) {
		deep = target;

		// skip the boolean and the target
		target = arguments[ i ] || {};
		i++;
	}

	// Handle case when target is a string or something (possible in deep copy)
	if ( typeof target !== "object" && !jQuery.isFunction(target) ) {
		target = {};
	}

	// extend jQuery itself if only one argument is passed
	if ( i === length ) {
		target = this;
		i--;
	}

	for ( ; i < length; i++ ) {
		// Only deal with non-null/undefined values
		if ( (options = arguments[ i ]) != null ) {
			// Extend the base object
			for ( name in options ) {
				src = target[ name ];
				copy = options[ name ];

				// Prevent never-ending loop
				if ( target === copy ) {
					continue;
				}

				// Recurse if we're merging plain objects or arrays
				if ( deep && copy && ( jQuery.isPlainObject(copy) || (copyIsArray = jQuery.isArray(copy)) ) ) {
					if ( copyIsArray ) {
						copyIsArray = false;
						clone = src && jQuery.isArray(src) ? src : [];

					} else {
						clone = src && jQuery.isPlainObject(src) ? src : {};
					}

					// Never move original objects, clone them
					target[ name ] = jQuery.extend( deep, clone, copy );

				// Don't bring in undefined values
				} else if ( copy !== undefined ) {
					target[ name ] = copy;
				}
			}
		}
	}

	// Return the modified object
	return target;
};

jQuery.extend({
	// Unique for each copy of jQuery on the page
	expando: "jQuery" + ( version + Math.random() ).replace( /\D/g, "" ),

	// Assume jQuery is ready without the ready module
	isReady: true,

	error: function( msg ) {
		throw new Error( msg );
	},

	noop: function() {},

	// See test/unit/core.js for details concerning isFunction.
	// Since version 1.3, DOM methods and functions like alert
	// aren't supported. They return false on IE (#2968).
	isFunction: function( obj ) {
		return jQuery.type(obj) === "function";
	},

	isArray: Array.isArray,

	isWindow: function( obj ) {
		return obj != null && obj === obj.window;
	},

	isNumeric: function( obj ) {
		// parseFloat NaNs numeric-cast false positives (null|true|false|"")
		// ...but misinterprets leading-number strings, particularly hex literals ("0x...")
		// subtraction forces infinities to NaN
		return !jQuery.isArray( obj ) && obj - parseFloat( obj ) >= 0;
	},

	isPlainObject: function( obj ) {
		// Not plain objects:
		// - Any object or value whose internal [[Class]] property is not "[object Object]"
		// - DOM nodes
		// - window
		if ( jQuery.type( obj ) !== "object" || obj.nodeType || jQuery.isWindow( obj ) ) {
			return false;
		}

		if ( obj.constructor &&
				!hasOwn.call( obj.constructor.prototype, "isPrototypeOf" ) ) {
			return false;
		}

		// If the function hasn't returned already, we're confident that
		// |obj| is a plain object, created by {} or constructed with new Object
		return true;
	},

	isEmptyObject: function( obj ) {
		var name;
		for ( name in obj ) {
			return false;
		}
		return true;
	},

	type: function( obj ) {
		if ( obj == null ) {
			return obj + "";
		}
		// Support: Android < 4.0, iOS < 6 (functionish RegExp)
		return typeof obj === "object" || typeof obj === "function" ?
			class2type[ toString.call(obj) ] || "object" :
			typeof obj;
	},

	// Evaluates a script in a global context
	globalEval: function( code ) {
		var script,
			indirect = eval;

		code = jQuery.trim( code );

		if ( code ) {
			// If the code includes a valid, prologue position
			// strict mode pragma, execute code by injecting a
			// script tag into the document.
			if ( code.indexOf("use strict") === 1 ) {
				script = document.createElement("script");
				script.text = code;
				document.head.appendChild( script ).parentNode.removeChild( script );
			} else {
			// Otherwise, avoid the DOM node creation, insertion
			// and removal by using an indirect global eval
				indirect( code );
			}
		}
	},

	// Convert dashed to camelCase; used by the css and data modules
	// Microsoft forgot to hump their vendor prefix (#9572)
	camelCase: function( string ) {
		return string.replace( rmsPrefix, "ms-" ).replace( rdashAlpha, fcamelCase );
	},

	nodeName: function( elem, name ) {
		return elem.nodeName && elem.nodeName.toLowerCase() === name.toLowerCase();
	},

	// args is for internal usage only
	each: function( obj, callback, args ) {
		var value,
			i = 0,
			length = obj.length,
			isArray = isArraylike( obj );

		if ( args ) {
			if ( isArray ) {
				for ( ; i < length; i++ ) {
					value = callback.apply( obj[ i ], args );

					if ( value === false ) {
						break;
					}
				}
			} else {
				for ( i in obj ) {
					value = callback.apply( obj[ i ], args );

					if ( value === false ) {
						break;
					}
				}
			}

		// A special, fast, case for the most common use of each
		} else {
			if ( isArray ) {
				for ( ; i < length; i++ ) {
					value = callback.call( obj[ i ], i, obj[ i ] );

					if ( value === false ) {
						break;
					}
				}
			} else {
				for ( i in obj ) {
					value = callback.call( obj[ i ], i, obj[ i ] );

					if ( value === false ) {
						break;
					}
				}
			}
		}

		return obj;
	},

	// Support: Android<4.1
	trim: function( text ) {
		return text == null ?
			"" :
			( text + "" ).replace( rtrim, "" );
	},

	// results is for internal usage only
	makeArray: function( arr, results ) {
		var ret = results || [];

		if ( arr != null ) {
			if ( isArraylike( Object(arr) ) ) {
				jQuery.merge( ret,
					typeof arr === "string" ?
					[ arr ] : arr
				);
			} else {
				push.call( ret, arr );
			}
		}

		return ret;
	},

	inArray: function( elem, arr, i ) {
		return arr == null ? -1 : indexOf.call( arr, elem, i );
	},

	merge: function( first, second ) {
		var len = +second.length,
			j = 0,
			i = first.length;

		for ( ; j < len; j++ ) {
			first[ i++ ] = second[ j ];
		}

		first.length = i;

		return first;
	},

	grep: function( elems, callback, invert ) {
		var callbackInverse,
			matches = [],
			i = 0,
			length = elems.length,
			callbackExpect = !invert;

		// Go through the array, only saving the items
		// that pass the validator function
		for ( ; i < length; i++ ) {
			callbackInverse = !callback( elems[ i ], i );
			if ( callbackInverse !== callbackExpect ) {
				matches.push( elems[ i ] );
			}
		}

		return matches;
	},

	// arg is for internal usage only
	map: function( elems, callback, arg ) {
		var value,
			i = 0,
			length = elems.length,
			isArray = isArraylike( elems ),
			ret = [];

		// Go through the array, translating each of the items to their new values
		if ( isArray ) {
			for ( ; i < length; i++ ) {
				value = callback( elems[ i ], i, arg );

				if ( value != null ) {
					ret.push( value );
				}
			}

		// Go through every key on the object,
		} else {
			for ( i in elems ) {
				value = callback( elems[ i ], i, arg );

				if ( value != null ) {
					ret.push( value );
				}
			}
		}

		// Flatten any nested arrays
		return concat.apply( [], ret );
	},

	// A global GUID counter for objects
	guid: 1,

	// Bind a function to a context, optionally partially applying any
	// arguments.
	proxy: function( fn, context ) {
		var tmp, args, proxy;

		if ( typeof context === "string" ) {
			tmp = fn[ context ];
			context = fn;
			fn = tmp;
		}

		// Quick check to determine if target is callable, in the spec
		// this throws a TypeError, but we will just return undefined.
		if ( !jQuery.isFunction( fn ) ) {
			return undefined;
		}

		// Simulated bind
		args = slice.call( arguments, 2 );
		proxy = function() {
			return fn.apply( context || this, args.concat( slice.call( arguments ) ) );
		};

		// Set the guid of unique handler to the same of original handler, so it can be removed
		proxy.guid = fn.guid = fn.guid || jQuery.guid++;

		return proxy;
	},

	now: Date.now,

	// jQuery.support is not used in Core but other projects attach their
	// properties to it so it needs to exist.
	support: support
});

// Populate the class2type map
jQuery.each("Boolean Number String Function Array Date RegExp Object Error".split(" "), function(i, name) {
	class2type[ "[object " + name + "]" ] = name.toLowerCase();
});

function isArraylike( obj ) {
	var length = obj.length,
		type = jQuery.type( obj );

	if ( type === "function" || jQuery.isWindow( obj ) ) {
		return false;
	}

	if ( obj.nodeType === 1 && length ) {
		return true;
	}

	return type === "array" || length === 0 ||
		typeof length === "number" && length > 0 && ( length - 1 ) in obj;
}
var Sizzle =
/*!
 * Sizzle CSS Selector Engine v1.10.19
 * http://sizzlejs.com/
 *
 * Copyright 2013 jQuery Foundation, Inc. and other contributors
 * Released under the MIT license
 * http://jquery.org/license
 *
 * Date: 2014-04-18
 */
(function( window ) {

var i,
	support,
	Expr,
	getText,
	isXML,
	tokenize,
	compile,
	select,
	outermostContext,
	sortInput,
	hasDuplicate,

	// Local document vars
	setDocument,
	document,
	docElem,
	documentIsHTML,
	rbuggyQSA,
	rbuggyMatches,
	matches,
	contains,

	// Instance-specific data
	expando = "sizzle" + -(new Date()),
	preferredDoc = window.document,
	dirruns = 0,
	done = 0,
	classCache = createCache(),
	tokenCache = createCache(),
	compilerCache = createCache(),
	sortOrder = function( a, b ) {
		if ( a === b ) {
			hasDuplicate = true;
		}
		return 0;
	},

	// General-purpose constants
	strundefined = typeof undefined,
	MAX_NEGATIVE = 1 << 31,

	// Instance methods
	hasOwn = ({}).hasOwnProperty,
	arr = [],
	pop = arr.pop,
	push_native = arr.push,
	push = arr.push,
	slice = arr.slice,
	// Use a stripped-down indexOf if we can't use a native one
	indexOf = arr.indexOf || function( elem ) {
		var i = 0,
			len = this.length;
		for ( ; i < len; i++ ) {
			if ( this[i] === elem ) {
				return i;
			}
		}
		return -1;
	},

	booleans = "checked|selected|async|autofocus|autoplay|controls|defer|disabled|hidden|ismap|loop|multiple|open|readonly|required|scoped",

	// Regular expressions

	// Whitespace characters http://www.w3.org/TR/css3-selectors/#whitespace
	whitespace = "[\\x20\\t\\r\\n\\f]",
	// http://www.w3.org/TR/css3-syntax/#characters
	characterEncoding = "(?:\\\\.|[\\w-]|[^\\x00-\\xa0])+",

	// Loosely modeled on CSS identifier characters
	// An unquoted value should be a CSS identifier http://www.w3.org/TR/css3-selectors/#attribute-selectors
	// Proper syntax: http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier
	identifier = characterEncoding.replace( "w", "w#" ),

	// Attribute selectors: http://www.w3.org/TR/selectors/#attribute-selectors
	attributes = "\\[" + whitespace + "*(" + characterEncoding + ")(?:" + whitespace +
		// Operator (capture 2)
		"*([*^$|!~]?=)" + whitespace +
		// "Attribute values must be CSS identifiers [capture 5] or strings [capture 3 or capture 4]"
		"*(?:'((?:\\\\.|[^\\\\'])*)'|\"((?:\\\\.|[^\\\\\"])*)\"|(" + identifier + "))|)" + whitespace +
		"*\\]",

	pseudos = ":(" + characterEncoding + ")(?:\\((" +
		// To reduce the number of selectors needing tokenize in the preFilter, prefer arguments:
		// 1. quoted (capture 3; capture 4 or capture 5)
		"('((?:\\\\.|[^\\\\'])*)'|\"((?:\\\\.|[^\\\\\"])*)\")|" +
		// 2. simple (capture 6)
		"((?:\\\\.|[^\\\\()[\\]]|" + attributes + ")*)|" +
		// 3. anything else (capture 2)
		".*" +
		")\\)|)",

	// Leading and non-escaped trailing whitespace, capturing some non-whitespace characters preceding the latter
	rtrim = new RegExp( "^" + whitespace + "+|((?:^|[^\\\\])(?:\\\\.)*)" + whitespace + "+$", "g" ),

	rcomma = new RegExp( "^" + whitespace + "*," + whitespace + "*" ),
	rcombinators = new RegExp( "^" + whitespace + "*([>+~]|" + whitespace + ")" + whitespace + "*" ),

	rattributeQuotes = new RegExp( "=" + whitespace + "*([^\\]'\"]*?)" + whitespace + "*\\]", "g" ),

	rpseudo = new RegExp( pseudos ),
	ridentifier = new RegExp( "^" + identifier + "$" ),

	matchExpr = {
		"ID": new RegExp( "^#(" + characterEncoding + ")" ),
		"CLASS": new RegExp( "^\\.(" + characterEncoding + ")" ),
		"TAG": new RegExp( "^(" + characterEncoding.replace( "w", "w*" ) + ")" ),
		"ATTR": new RegExp( "^" + attributes ),
		"PSEUDO": new RegExp( "^" + pseudos ),
		"CHILD": new RegExp( "^:(only|first|last|nth|nth-last)-(child|of-type)(?:\\(" + whitespace +
			"*(even|odd|(([+-]|)(\\d*)n|)" + whitespace + "*(?:([+-]|)" + whitespace +
			"*(\\d+)|))" + whitespace + "*\\)|)", "i" ),
		"bool": new RegExp( "^(?:" + booleans + ")$", "i" ),
		// For use in libraries implementing .is()
		// We use this for POS matching in `select`
		"needsContext": new RegExp( "^" + whitespace + "*[>+~]|:(even|odd|eq|gt|lt|nth|first|last)(?:\\(" +
			whitespace + "*((?:-\\d)?\\d*)" + whitespace + "*\\)|)(?=[^-]|$)", "i" )
	},

	rinputs = /^(?:input|select|textarea|button)$/i,
	rheader = /^h\d$/i,

	rnative = /^[^{]+\{\s*\[native \w/,

	// Easily-parseable/retrievable ID or TAG or CLASS selectors
	rquickExpr = /^(?:#([\w-]+)|(\w+)|\.([\w-]+))$/,

	rsibling = /[+~]/,
	rescape = /'|\\/g,

	// CSS escapes http://www.w3.org/TR/CSS21/syndata.html#escaped-characters
	runescape = new RegExp( "\\\\([\\da-f]{1,6}" + whitespace + "?|(" + whitespace + ")|.)", "ig" ),
	funescape = function( _, escaped, escapedWhitespace ) {
		var high = "0x" + escaped - 0x10000;
		// NaN means non-codepoint
		// Support: Firefox<24
		// Workaround erroneous numeric interpretation of +"0x"
		return high !== high || escapedWhitespace ?
			escaped :
			high < 0 ?
				// BMP codepoint
				String.fromCharCode( high + 0x10000 ) :
				// Supplemental Plane codepoint (surrogate pair)
				String.fromCharCode( high >> 10 | 0xD800, high & 0x3FF | 0xDC00 );
	};

// Optimize for push.apply( _, NodeList )
try {
	push.apply(
		(arr = slice.call( preferredDoc.childNodes )),
		preferredDoc.childNodes
	);
	// Support: Android<4.0
	// Detect silently failing push.apply
	arr[ preferredDoc.childNodes.length ].nodeType;
} catch ( e ) {
	push = { apply: arr.length ?

		// Leverage slice if possible
		function( target, els ) {
			push_native.apply( target, slice.call(els) );
		} :

		// Support: IE<9
		// Otherwise append directly
		function( target, els ) {
			var j = target.length,
				i = 0;
			// Can't trust NodeList.length
			while ( (target[j++] = els[i++]) ) {}
			target.length = j - 1;
		}
	};
}

function Sizzle( selector, context, results, seed ) {
	var match, elem, m, nodeType,
		// QSA vars
		i, groups, old, nid, newContext, newSelector;

	if ( ( context ? context.ownerDocument || context : preferredDoc ) !== document ) {
		setDocument( context );
	}

	context = context || document;
	results = results || [];

	if ( !selector || typeof selector !== "string" ) {
		return results;
	}

	if ( (nodeType = context.nodeType) !== 1 && nodeType !== 9 ) {
		return [];
	}

	if ( documentIsHTML && !seed ) {

		// Shortcuts
		if ( (match = rquickExpr.exec( selector )) ) {
			// Speed-up: Sizzle("#ID")
			if ( (m = match[1]) ) {
				if ( nodeType === 9 ) {
					elem = context.getElementById( m );
					// Check parentNode to catch when Blackberry 4.6 returns
					// nodes that are no longer in the document (jQuery #6963)
					if ( elem && elem.parentNode ) {
						// Handle the case where IE, Opera, and Webkit return items
						// by name instead of ID
						if ( elem.id === m ) {
							results.push( elem );
							return results;
						}
					} else {
						return results;
					}
				} else {
					// Context is not a document
					if ( context.ownerDocument && (elem = context.ownerDocument.getElementById( m )) &&
						contains( context, elem ) && elem.id === m ) {
						results.push( elem );
						return results;
					}
				}

			// Speed-up: Sizzle("TAG")
			} else if ( match[2] ) {
				push.apply( results, context.getElementsByTagName( selector ) );
				return results;

			// Speed-up: Sizzle(".CLASS")
			} else if ( (m = match[3]) && support.getElementsByClassName && context.getElementsByClassName ) {
				push.apply( results, context.getElementsByClassName( m ) );
				return results;
			}
		}

		// QSA path
		if ( support.qsa && (!rbuggyQSA || !rbuggyQSA.test( selector )) ) {
			nid = old = expando;
			newContext = context;
			newSelector = nodeType === 9 && selector;

			// qSA works strangely on Element-rooted queries
			// We can work around this by specifying an extra ID on the root
			// and working up from there (Thanks to Andrew Dupont for the technique)
			// IE 8 doesn't work on object elements
			if ( nodeType === 1 && context.nodeName.toLowerCase() !== "object" ) {
				groups = tokenize( selector );

				if ( (old = context.getAttribute("id")) ) {
					nid = old.replace( rescape, "\\$&" );
				} else {
					context.setAttribute( "id", nid );
				}
				nid = "[id='" + nid + "'] ";

				i = groups.length;
				while ( i-- ) {
					groups[i] = nid + toSelector( groups[i] );
				}
				newContext = rsibling.test( selector ) && testContext( context.parentNode ) || context;
				newSelector = groups.join(",");
			}

			if ( newSelector ) {
				try {
					push.apply( results,
						newContext.querySelectorAll( newSelector )
					);
					return results;
				} catch(qsaError) {
				} finally {
					if ( !old ) {
						context.removeAttribute("id");
					}
				}
			}
		}
	}

	// All others
	return select( selector.replace( rtrim, "$1" ), context, results, seed );
}

/**
 * Create key-value caches of limited size
 * @returns {Function(string, Object)} Returns the Object data after storing it on itself with
 *	property name the (space-suffixed) string and (if the cache is larger than Expr.cacheLength)
 *	deleting the oldest entry
 */
function createCache() {
	var keys = [];

	function cache( key, value ) {
		// Use (key + " ") to avoid collision with native prototype properties (see Issue #157)
		if ( keys.push( key + " " ) > Expr.cacheLength ) {
			// Only keep the most recent entries
			delete cache[ keys.shift() ];
		}
		return (cache[ key + " " ] = value);
	}
	return cache;
}

/**
 * Mark a function for special use by Sizzle
 * @param {Function} fn The function to mark
 */
function markFunction( fn ) {
	fn[ expando ] = true;
	return fn;
}

/**
 * Support testing using an element
 * @param {Function} fn Passed the created div and expects a boolean result
 */
function assert( fn ) {
	var div = document.createElement("div");

	try {
		return !!fn( div );
	} catch (e) {
		return false;
	} finally {
		// Remove from its parent by default
		if ( div.parentNode ) {
			div.parentNode.removeChild( div );
		}
		// release memory in IE
		div = null;
	}
}

/**
 * Adds the same handler for all of the specified attrs
 * @param {String} attrs Pipe-separated list of attributes
 * @param {Function} handler The method that will be applied
 */
function addHandle( attrs, handler ) {
	var arr = attrs.split("|"),
		i = attrs.length;

	while ( i-- ) {
		Expr.attrHandle[ arr[i] ] = handler;
	}
}

/**
 * Checks document order of two siblings
 * @param {Element} a
 * @param {Element} b
 * @returns {Number} Returns less than 0 if a precedes b, greater than 0 if a follows b
 */
function siblingCheck( a, b ) {
	var cur = b && a,
		diff = cur && a.nodeType === 1 && b.nodeType === 1 &&
			( ~b.sourceIndex || MAX_NEGATIVE ) -
			( ~a.sourceIndex || MAX_NEGATIVE );

	// Use IE sourceIndex if available on both nodes
	if ( diff ) {
		return diff;
	}

	// Check if b follows a
	if ( cur ) {
		while ( (cur = cur.nextSibling) ) {
			if ( cur === b ) {
				return -1;
			}
		}
	}

	return a ? 1 : -1;
}

/**
 * Returns a function to use in pseudos for input types
 * @param {String} type
 */
function createInputPseudo( type ) {
	return function( elem ) {
		var name = elem.nodeName.toLowerCase();
		return name === "input" && elem.type === type;
	};
}

/**
 * Returns a function to use in pseudos for buttons
 * @param {String} type
 */
function createButtonPseudo( type ) {
	return function( elem ) {
		var name = elem.nodeName.toLowerCase();
		return (name === "input" || name === "button") && elem.type === type;
	};
}

/**
 * Returns a function to use in pseudos for positionals
 * @param {Function} fn
 */
function createPositionalPseudo( fn ) {
	return markFunction(function( argument ) {
		argument = +argument;
		return markFunction(function( seed, matches ) {
			var j,
				matchIndexes = fn( [], seed.length, argument ),
				i = matchIndexes.length;

			// Match elements found at the specified indexes
			while ( i-- ) {
				if ( seed[ (j = matchIndexes[i]) ] ) {
					seed[j] = !(matches[j] = seed[j]);
				}
			}
		});
	});
}

/**
 * Checks a node for validity as a Sizzle context
 * @param {Element|Object=} context
 * @returns {Element|Object|Boolean} The input node if acceptable, otherwise a falsy value
 */
function testContext( context ) {
	return context && typeof context.getElementsByTagName !== strundefined && context;
}

// Expose support vars for convenience
support = Sizzle.support = {};

/**
 * Detects XML nodes
 * @param {Element|Object} elem An element or a document
 * @returns {Boolean} True iff elem is a non-HTML XML node
 */
isXML = Sizzle.isXML = function( elem ) {
	// documentElement is verified for cases where it doesn't yet exist
	// (such as loading iframes in IE - #4833)
	var documentElement = elem && (elem.ownerDocument || elem).documentElement;
	return documentElement ? documentElement.nodeName !== "HTML" : false;
};

/**
 * Sets document-related variables once based on the current document
 * @param {Element|Object} [doc] An element or document object to use to set the document
 * @returns {Object} Returns the current document
 */
setDocument = Sizzle.setDocument = function( node ) {
	var hasCompare,
		doc = node ? node.ownerDocument || node : preferredDoc,
		parent = doc.defaultView;

	// If no document and documentElement is available, return
	if ( doc === document || doc.nodeType !== 9 || !doc.documentElement ) {
		return document;
	}

	// Set our document
	document = doc;
	docElem = doc.documentElement;

	// Support tests
	documentIsHTML = !isXML( doc );

	// Support: IE>8
	// If iframe document is assigned to "document" variable and if iframe has been reloaded,
	// IE will throw "permission denied" error when accessing "document" variable, see jQuery #13936
	// IE6-8 do not support the defaultView property so parent will be undefined
	if ( parent && parent !== parent.top ) {
		// IE11 does not have attachEvent, so all must suffer
		if ( parent.addEventListener ) {
			parent.addEventListener( "unload", function() {
				setDocument();
			}, false );
		} else if ( parent.attachEvent ) {
			parent.attachEvent( "onunload", function() {
				setDocument();
			});
		}
	}

	/* Attributes
	---------------------------------------------------------------------- */

	// Support: IE<8
	// Verify that getAttribute really returns attributes and not properties (excepting IE8 booleans)
	support.attributes = assert(function( div ) {
		div.className = "i";
		return !div.getAttribute("className");
	});

	/* getElement(s)By*
	---------------------------------------------------------------------- */

	// Check if getElementsByTagName("*") returns only elements
	support.getElementsByTagName = assert(function( div ) {
		div.appendChild( doc.createComment("") );
		return !div.getElementsByTagName("*").length;
	});

	// Check if getElementsByClassName can be trusted
	support.getElementsByClassName = rnative.test( doc.getElementsByClassName ) && assert(function( div ) {
		div.innerHTML = "<div class='a'></div><div class='a i'></div>";

		// Support: Safari<4
		// Catch class over-caching
		div.firstChild.className = "i";
		// Support: Opera<10
		// Catch gEBCN failure to find non-leading classes
		return div.getElementsByClassName("i").length === 2;
	});

	// Support: IE<10
	// Check if getElementById returns elements by name
	// The broken getElementById methods don't pick up programatically-set names,
	// so use a roundabout getElementsByName test
	support.getById = assert(function( div ) {
		docElem.appendChild( div ).id = expando;
		return !doc.getElementsByName || !doc.getElementsByName( expando ).length;
	});

	// ID find and filter
	if ( support.getById ) {
		Expr.find["ID"] = function( id, context ) {
			if ( typeof context.getElementById !== strundefined && documentIsHTML ) {
				var m = context.getElementById( id );
				// Check parentNode to catch when Blackberry 4.6 returns
				// nodes that are no longer in the document #6963
				return m && m.parentNode ? [ m ] : [];
			}
		};
		Expr.filter["ID"] = function( id ) {
			var attrId = id.replace( runescape, funescape );
			return function( elem ) {
				return elem.getAttribute("id") === attrId;
			};
		};
	} else {
		// Support: IE6/7
		// getElementById is not reliable as a find shortcut
		delete Expr.find["ID"];

		Expr.filter["ID"] =  function( id ) {
			var attrId = id.replace( runescape, funescape );
			return function( elem ) {
				var node = typeof elem.getAttributeNode !== strundefined && elem.getAttributeNode("id");
				return node && node.value === attrId;
			};
		};
	}

	// Tag
	Expr.find["TAG"] = support.getElementsByTagName ?
		function( tag, context ) {
			if ( typeof context.getElementsByTagName !== strundefined ) {
				return context.getElementsByTagName( tag );
			}
		} :
		function( tag, context ) {
			var elem,
				tmp = [],
				i = 0,
				results = context.getElementsByTagName( tag );

			// Filter out possible comments
			if ( tag === "*" ) {
				while ( (elem = results[i++]) ) {
					if ( elem.nodeType === 1 ) {
						tmp.push( elem );
					}
				}

				return tmp;
			}
			return results;
		};

	// Class
	Expr.find["CLASS"] = support.getElementsByClassName && function( className, context ) {
		if ( typeof context.getElementsByClassName !== strundefined && documentIsHTML ) {
			return context.getElementsByClassName( className );
		}
	};

	/* QSA/matchesSelector
	---------------------------------------------------------------------- */

	// QSA and matchesSelector support

	// matchesSelector(:active) reports false when true (IE9/Opera 11.5)
	rbuggyMatches = [];

	// qSa(:focus) reports false when true (Chrome 21)
	// We allow this because of a bug in IE8/9 that throws an error
	// whenever `document.activeElement` is accessed on an iframe
	// So, we allow :focus to pass through QSA all the time to avoid the IE error
	// See http://bugs.jquery.com/ticket/13378
	rbuggyQSA = [];

	if ( (support.qsa = rnative.test( doc.querySelectorAll )) ) {
		// Build QSA regex
		// Regex strategy adopted from Diego Perini
		assert(function( div ) {
			// Select is set to empty string on purpose
			// This is to test IE's treatment of not explicitly
			// setting a boolean content attribute,
			// since its presence should be enough
			// http://bugs.jquery.com/ticket/12359
			div.innerHTML = "<select msallowclip=''><option selected=''></option></select>";

			// Support: IE8, Opera 11-12.16
			// Nothing should be selected when empty strings follow ^= or $= or *=
			// The test attribute must be unknown in Opera but "safe" for WinRT
			// http://msdn.microsoft.com/en-us/library/ie/hh465388.aspx#attribute_section
			if ( div.querySelectorAll("[msallowclip^='']").length ) {
				rbuggyQSA.push( "[*^$]=" + whitespace + "*(?:''|\"\")" );
			}

			// Support: IE8
			// Boolean attributes and "value" are not treated correctly
			if ( !div.querySelectorAll("[selected]").length ) {
				rbuggyQSA.push( "\\[" + whitespace + "*(?:value|" + booleans + ")" );
			}

			// Webkit/Opera - :checked should return selected option elements
			// http://www.w3.org/TR/2011/REC-css3-selectors-20110929/#checked
			// IE8 throws error here and will not see later tests
			if ( !div.querySelectorAll(":checked").length ) {
				rbuggyQSA.push(":checked");
			}
		});

		assert(function( div ) {
			// Support: Windows 8 Native Apps
			// The type and name attributes are restricted during .innerHTML assignment
			var input = doc.createElement("input");
			input.setAttribute( "type", "hidden" );
			div.appendChild( input ).setAttribute( "name", "D" );

			// Support: IE8
			// Enforce case-sensitivity of name attribute
			if ( div.querySelectorAll("[name=d]").length ) {
				rbuggyQSA.push( "name" + whitespace + "*[*^$|!~]?=" );
			}

			// FF 3.5 - :enabled/:disabled and hidden elements (hidden elements are still enabled)
			// IE8 throws error here and will not see later tests
			if ( !div.querySelectorAll(":enabled").length ) {
				rbuggyQSA.push( ":enabled", ":disabled" );
			}

			// Opera 10-11 does not throw on post-comma invalid pseudos
			div.querySelectorAll("*,:x");
			rbuggyQSA.push(",.*:");
		});
	}

	if ( (support.matchesSelector = rnative.test( (matches = docElem.matches ||
		docElem.webkitMatchesSelector ||
		docElem.mozMatchesSelector ||
		docElem.oMatchesSelector ||
		docElem.msMatchesSelector) )) ) {

		assert(function( div ) {
			// Check to see if it's possible to do matchesSelector
			// on a disconnected node (IE 9)
			support.disconnectedMatch = matches.call( div, "div" );

			// This should fail with an exception
			// Gecko does not error, returns false instead
			matches.call( div, "[s!='']:x" );
			rbuggyMatches.push( "!=", pseudos );
		});
	}

	rbuggyQSA = rbuggyQSA.length && new RegExp( rbuggyQSA.join("|") );
	rbuggyMatches = rbuggyMatches.length && new RegExp( rbuggyMatches.join("|") );

	/* Contains
	---------------------------------------------------------------------- */
	hasCompare = rnative.test( docElem.compareDocumentPosition );

	// Element contains another
	// Purposefully does not implement inclusive descendent
	// As in, an element does not contain itself
	contains = hasCompare || rnative.test( docElem.contains ) ?
		function( a, b ) {
			var adown = a.nodeType === 9 ? a.documentElement : a,
				bup = b && b.parentNode;
			return a === bup || !!( bup && bup.nodeType === 1 && (
				adown.contains ?
					adown.contains( bup ) :
					a.compareDocumentPosition && a.compareDocumentPosition( bup ) & 16
			));
		} :
		function( a, b ) {
			if ( b ) {
				while ( (b = b.parentNode) ) {
					if ( b === a ) {
						return true;
					}
				}
			}
			return false;
		};

	/* Sorting
	---------------------------------------------------------------------- */

	// Document order sorting
	sortOrder = hasCompare ?
	function( a, b ) {

		// Flag for duplicate removal
		if ( a === b ) {
			hasDuplicate = true;
			return 0;
		}

		// Sort on method existence if only one input has compareDocumentPosition
		var compare = !a.compareDocumentPosition - !b.compareDocumentPosition;
		if ( compare ) {
			return compare;
		}

		// Calculate position if both inputs belong to the same document
		compare = ( a.ownerDocument || a ) === ( b.ownerDocument || b ) ?
			a.compareDocumentPosition( b ) :

			// Otherwise we know they are disconnected
			1;

		// Disconnected nodes
		if ( compare & 1 ||
			(!support.sortDetached && b.compareDocumentPosition( a ) === compare) ) {

			// Choose the first element that is related to our preferred document
			if ( a === doc || a.ownerDocument === preferredDoc && contains(preferredDoc, a) ) {
				return -1;
			}
			if ( b === doc || b.ownerDocument === preferredDoc && contains(preferredDoc, b) ) {
				return 1;
			}

			// Maintain original order
			return sortInput ?
				( indexOf.call( sortInput, a ) - indexOf.call( sortInput, b ) ) :
				0;
		}

		return compare & 4 ? -1 : 1;
	} :
	function( a, b ) {
		// Exit early if the nodes are identical
		if ( a === b ) {
			hasDuplicate = true;
			return 0;
		}

		var cur,
			i = 0,
			aup = a.parentNode,
			bup = b.parentNode,
			ap = [ a ],
			bp = [ b ];

		// Parentless nodes are either documents or disconnected
		if ( !aup || !bup ) {
			return a === doc ? -1 :
				b === doc ? 1 :
				aup ? -1 :
				bup ? 1 :
				sortInput ?
				( indexOf.call( sortInput, a ) - indexOf.call( sortInput, b ) ) :
				0;

		// If the nodes are siblings, we can do a quick check
		} else if ( aup === bup ) {
			return siblingCheck( a, b );
		}

		// Otherwise we need full lists of their ancestors for comparison
		cur = a;
		while ( (cur = cur.parentNode) ) {
			ap.unshift( cur );
		}
		cur = b;
		while ( (cur = cur.parentNode) ) {
			bp.unshift( cur );
		}

		// Walk down the tree looking for a discrepancy
		while ( ap[i] === bp[i] ) {
			i++;
		}

		return i ?
			// Do a sibling check if the nodes have a common ancestor
			siblingCheck( ap[i], bp[i] ) :

			// Otherwise nodes in our document sort first
			ap[i] === preferredDoc ? -1 :
			bp[i] === preferredDoc ? 1 :
			0;
	};

	return doc;
};

Sizzle.matches = function( expr, elements ) {
	return Sizzle( expr, null, null, elements );
};

Sizzle.matchesSelector = function( elem, expr ) {
	// Set document vars if needed
	if ( ( elem.ownerDocument || elem ) !== document ) {
		setDocument( elem );
	}

	// Make sure that attribute selectors are quoted
	expr = expr.replace( rattributeQuotes, "='$1']" );

	if ( support.matchesSelector && documentIsHTML &&
		( !rbuggyMatches || !rbuggyMatches.test( expr ) ) &&
		( !rbuggyQSA     || !rbuggyQSA.test( expr ) ) ) {

		try {
			var ret = matches.call( elem, expr );

			// IE 9's matchesSelector returns false on disconnected nodes
			if ( ret || support.disconnectedMatch ||
					// As well, disconnected nodes are said to be in a document
					// fragment in IE 9
					elem.document && elem.document.nodeType !== 11 ) {
				return ret;
			}
		} catch(e) {}
	}

	return Sizzle( expr, document, null, [ elem ] ).length > 0;
};

Sizzle.contains = function( context, elem ) {
	// Set document vars if needed
	if ( ( context.ownerDocument || context ) !== document ) {
		setDocument( context );
	}
	return contains( context, elem );
};

Sizzle.attr = function( elem, name ) {
	// Set document vars if needed
	if ( ( elem.ownerDocument || elem ) !== document ) {
		setDocument( elem );
	}

	var fn = Expr.attrHandle[ name.toLowerCase() ],
		// Don't get fooled by Object.prototype properties (jQuery #13807)
		val = fn && hasOwn.call( Expr.attrHandle, name.toLowerCase() ) ?
			fn( elem, name, !documentIsHTML ) :
			undefined;

	return val !== undefined ?
		val :
		support.attributes || !documentIsHTML ?
			elem.getAttribute( name ) :
			(val = elem.getAttributeNode(name)) && val.specified ?
				val.value :
				null;
};

Sizzle.error = function( msg ) {
	throw new Error( "Syntax error, unrecognized expression: " + msg );
};

/**
 * Document sorting and removing duplicates
 * @param {ArrayLike} results
 */
Sizzle.uniqueSort = function( results ) {
	var elem,
		duplicates = [],
		j = 0,
		i = 0;

	// Unless we *know* we can detect duplicates, assume their presence
	hasDuplicate = !support.detectDuplicates;
	sortInput = !support.sortStable && results.slice( 0 );
	results.sort( sortOrder );

	if ( hasDuplicate ) {
		while ( (elem = results[i++]) ) {
			if ( elem === results[ i ] ) {
				j = duplicates.push( i );
			}
		}
		while ( j-- ) {
			results.splice( duplicates[ j ], 1 );
		}
	}

	// Clear input after sorting to release objects
	// See https://github.com/jquery/sizzle/pull/225
	sortInput = null;

	return results;
};

/**
 * Utility function for retrieving the text value of an array of DOM nodes
 * @param {Array|Element} elem
 */
getText = Sizzle.getText = function( elem ) {
	var node,
		ret = "",
		i = 0,
		nodeType = elem.nodeType;

	if ( !nodeType ) {
		// If no nodeType, this is expected to be an array
		while ( (node = elem[i++]) ) {
			// Do not traverse comment nodes
			ret += getText( node );
		}
	} else if ( nodeType === 1 || nodeType === 9 || nodeType === 11 ) {
		// Use textContent for elements
		// innerText usage removed for consistency of new lines (jQuery #11153)
		if ( typeof elem.textContent === "string" ) {
			return elem.textContent;
		} else {
			// Traverse its children
			for ( elem = elem.firstChild; elem; elem = elem.nextSibling ) {
				ret += getText( elem );
			}
		}
	} else if ( nodeType === 3 || nodeType === 4 ) {
		return elem.nodeValue;
	}
	// Do not include comment or processing instruction nodes

	return ret;
};

Expr = Sizzle.selectors = {

	// Can be adjusted by the user
	cacheLength: 50,

	createPseudo: markFunction,

	match: matchExpr,

	attrHandle: {},

	find: {},

	relative: {
		">": { dir: "parentNode", first: true },
		" ": { dir: "parentNode" },
		"+": { dir: "previousSibling", first: true },
		"~": { dir: "previousSibling" }
	},

	preFilter: {
		"ATTR": function( match ) {
			match[1] = match[1].replace( runescape, funescape );

			// Move the given value to match[3] whether quoted or unquoted
			match[3] = ( match[3] || match[4] || match[5] || "" ).replace( runescape, funescape );

			if ( match[2] === "~=" ) {
				match[3] = " " + match[3] + " ";
			}

			return match.slice( 0, 4 );
		},

		"CHILD": function( match ) {
			/* matches from matchExpr["CHILD"]
				1 type (only|nth|...)
				2 what (child|of-type)
				3 argument (even|odd|\d*|\d*n([+-]\d+)?|...)
				4 xn-component of xn+y argument ([+-]?\d*n|)
				5 sign of xn-component
				6 x of xn-component
				7 sign of y-component
				8 y of y-component
			*/
			match[1] = match[1].toLowerCase();

			if ( match[1].slice( 0, 3 ) === "nth" ) {
				// nth-* requires argument
				if ( !match[3] ) {
					Sizzle.error( match[0] );
				}

				// numeric x and y parameters for Expr.filter.CHILD
				// remember that false/true cast respectively to 0/1
				match[4] = +( match[4] ? match[5] + (match[6] || 1) : 2 * ( match[3] === "even" || match[3] === "odd" ) );
				match[5] = +( ( match[7] + match[8] ) || match[3] === "odd" );

			// other types prohibit arguments
			} else if ( match[3] ) {
				Sizzle.error( match[0] );
			}

			return match;
		},

		"PSEUDO": function( match ) {
			var excess,
				unquoted = !match[6] && match[2];

			if ( matchExpr["CHILD"].test( match[0] ) ) {
				return null;
			}

			// Accept quoted arguments as-is
			if ( match[3] ) {
				match[2] = match[4] || match[5] || "";

			// Strip excess characters from unquoted arguments
			} else if ( unquoted && rpseudo.test( unquoted ) &&
				// Get excess from tokenize (recursively)
				(excess = tokenize( unquoted, true )) &&
				// advance to the next closing parenthesis
				(excess = unquoted.indexOf( ")", unquoted.length - excess ) - unquoted.length) ) {

				// excess is a negative index
				match[0] = match[0].slice( 0, excess );
				match[2] = unquoted.slice( 0, excess );
			}

			// Return only captures needed by the pseudo filter method (type and argument)
			return match.slice( 0, 3 );
		}
	},

	filter: {

		"TAG": function( nodeNameSelector ) {
			var nodeName = nodeNameSelector.replace( runescape, funescape ).toLowerCase();
			return nodeNameSelector === "*" ?
				function() { return true; } :
				function( elem ) {
					return elem.nodeName && elem.nodeName.toLowerCase() === nodeName;
				};
		},

		"CLASS": function( className ) {
			var pattern = classCache[ className + " " ];

			return pattern ||
				(pattern = new RegExp( "(^|" + whitespace + ")" + className + "(" + whitespace + "|$)" )) &&
				classCache( className, function( elem ) {
					return pattern.test( typeof elem.className === "string" && elem.className || typeof elem.getAttribute !== strundefined && elem.getAttribute("class") || "" );
				});
		},

		"ATTR": function( name, operator, check ) {
			return function( elem ) {
				var result = Sizzle.attr( elem, name );

				if ( result == null ) {
					return operator === "!=";
				}
				if ( !operator ) {
					return true;
				}

				result += "";

				return operator === "=" ? result === check :
					operator === "!=" ? result !== check :
					operator === "^=" ? check && result.indexOf( check ) === 0 :
					operator === "*=" ? check && result.indexOf( check ) > -1 :
					operator === "$=" ? check && result.slice( -check.length ) === check :
					operator === "~=" ? ( " " + result + " " ).indexOf( check ) > -1 :
					operator === "|=" ? result === check || result.slice( 0, check.length + 1 ) === check + "-" :
					false;
			};
		},

		"CHILD": function( type, what, argument, first, last ) {
			var simple = type.slice( 0, 3 ) !== "nth",
				forward = type.slice( -4 ) !== "last",
				ofType = what === "of-type";

			return first === 1 && last === 0 ?

				// Shortcut for :nth-*(n)
				function( elem ) {
					return !!elem.parentNode;
				} :

				function( elem, context, xml ) {
					var cache, outerCache, node, diff, nodeIndex, start,
						dir = simple !== forward ? "nextSibling" : "previousSibling",
						parent = elem.parentNode,
						name = ofType && elem.nodeName.toLowerCase(),
						useCache = !xml && !ofType;

					if ( parent ) {

						// :(first|last|only)-(child|of-type)
						if ( simple ) {
							while ( dir ) {
								node = elem;
								while ( (node = node[ dir ]) ) {
									if ( ofType ? node.nodeName.toLowerCase() === name : node.nodeType === 1 ) {
										return false;
									}
								}
								// Reverse direction for :only-* (if we haven't yet done so)
								start = dir = type === "only" && !start && "nextSibling";
							}
							return true;
						}

						start = [ forward ? parent.firstChild : parent.lastChild ];

						// non-xml :nth-child(...) stores cache data on `parent`
						if ( forward && useCache ) {
							// Seek `elem` from a previously-cached index
							outerCache = parent[ expando ] || (parent[ expando ] = {});
							cache = outerCache[ type ] || [];
							nodeIndex = cache[0] === dirruns && cache[1];
							diff = cache[0] === dirruns && cache[2];
							node = nodeIndex && parent.childNodes[ nodeIndex ];

							while ( (node = ++nodeIndex && node && node[ dir ] ||

								// Fallback to seeking `elem` from the start
								(diff = nodeIndex = 0) || start.pop()) ) {

								// When found, cache indexes on `parent` and break
								if ( node.nodeType === 1 && ++diff && node === elem ) {
									outerCache[ type ] = [ dirruns, nodeIndex, diff ];
									break;
								}
							}

						// Use previously-cached element index if available
						} else if ( useCache && (cache = (elem[ expando ] || (elem[ expando ] = {}))[ type ]) && cache[0] === dirruns ) {
							diff = cache[1];

						// xml :nth-child(...) or :nth-last-child(...) or :nth(-last)?-of-type(...)
						} else {
							// Use the same loop as above to seek `elem` from the start
							while ( (node = ++nodeIndex && node && node[ dir ] ||
								(diff = nodeIndex = 0) || start.pop()) ) {

								if ( ( ofType ? node.nodeName.toLowerCase() === name : node.nodeType === 1 ) && ++diff ) {
									// Cache the index of each encountered element
									if ( useCache ) {
										(node[ expando ] || (node[ expando ] = {}))[ type ] = [ dirruns, diff ];
									}

									if ( node === elem ) {
										break;
									}
								}
							}
						}

						// Incorporate the offset, then check against cycle size
						diff -= last;
						return diff === first || ( diff % first === 0 && diff / first >= 0 );
					}
				};
		},

		"PSEUDO": function( pseudo, argument ) {
			// pseudo-class names are case-insensitive
			// http://www.w3.org/TR/selectors/#pseudo-classes
			// Prioritize by case sensitivity in case custom pseudos are added with uppercase letters
			// Remember that setFilters inherits from pseudos
			var args,
				fn = Expr.pseudos[ pseudo ] || Expr.setFilters[ pseudo.toLowerCase() ] ||
					Sizzle.error( "unsupported pseudo: " + pseudo );

			// The user may use createPseudo to indicate that
			// arguments are needed to create the filter function
			// just as Sizzle does
			if ( fn[ expando ] ) {
				return fn( argument );
			}

			// But maintain support for old signatures
			if ( fn.length > 1 ) {
				args = [ pseudo, pseudo, "", argument ];
				return Expr.setFilters.hasOwnProperty( pseudo.toLowerCase() ) ?
					markFunction(function( seed, matches ) {
						var idx,
							matched = fn( seed, argument ),
							i = matched.length;
						while ( i-- ) {
							idx = indexOf.call( seed, matched[i] );
							seed[ idx ] = !( matches[ idx ] = matched[i] );
						}
					}) :
					function( elem ) {
						return fn( elem, 0, args );
					};
			}

			return fn;
		}
	},

	pseudos: {
		// Potentially complex pseudos
		"not": markFunction(function( selector ) {
			// Trim the selector passed to compile
			// to avoid treating leading and trailing
			// spaces as combinators
			var input = [],
				results = [],
				matcher = compile( selector.replace( rtrim, "$1" ) );

			return matcher[ expando ] ?
				markFunction(function( seed, matches, context, xml ) {
					var elem,
						unmatched = matcher( seed, null, xml, [] ),
						i = seed.length;

					// Match elements unmatched by `matcher`
					while ( i-- ) {
						if ( (elem = unmatched[i]) ) {
							seed[i] = !(matches[i] = elem);
						}
					}
				}) :
				function( elem, context, xml ) {
					input[0] = elem;
					matcher( input, null, xml, results );
					return !results.pop();
				};
		}),

		"has": markFunction(function( selector ) {
			return function( elem ) {
				return Sizzle( selector, elem ).length > 0;
			};
		}),

		"contains": markFunction(function( text ) {
			return function( elem ) {
				return ( elem.textContent || elem.innerText || getText( elem ) ).indexOf( text ) > -1;
			};
		}),

		// "Whether an element is represented by a :lang() selector
		// is based solely on the element's language value
		// being equal to the identifier C,
		// or beginning with the identifier C immediately followed by "-".
		// The matching of C against the element's language value is performed case-insensitively.
		// The identifier C does not have to be a valid language name."
		// http://www.w3.org/TR/selectors/#lang-pseudo
		"lang": markFunction( function( lang ) {
			// lang value must be a valid identifier
			if ( !ridentifier.test(lang || "") ) {
				Sizzle.error( "unsupported lang: " + lang );
			}
			lang = lang.replace( runescape, funescape ).toLowerCase();
			return function( elem ) {
				var elemLang;
				do {
					if ( (elemLang = documentIsHTML ?
						elem.lang :
						elem.getAttribute("xml:lang") || elem.getAttribute("lang")) ) {

						elemLang = elemLang.toLowerCase();
						return elemLang === lang || elemLang.indexOf( lang + "-" ) === 0;
					}
				} while ( (elem = elem.parentNode) && elem.nodeType === 1 );
				return false;
			};
		}),

		// Miscellaneous
		"target": function( elem ) {
			var hash = window.location && window.location.hash;
			return hash && hash.slice( 1 ) === elem.id;
		},

		"root": function( elem ) {
			return elem === docElem;
		},

		"focus": function( elem ) {
			return elem === document.activeElement && (!document.hasFocus || document.hasFocus()) && !!(elem.type || elem.href || ~elem.tabIndex);
		},

		// Boolean properties
		"enabled": function( elem ) {
			return elem.disabled === false;
		},

		"disabled": function( elem ) {
			return elem.disabled === true;
		},

		"checked": function( elem ) {
			// In CSS3, :checked should return both checked and selected elements
			// http://www.w3.org/TR/2011/REC-css3-selectors-20110929/#checked
			var nodeName = elem.nodeName.toLowerCase();
			return (nodeName === "input" && !!elem.checked) || (nodeName === "option" && !!elem.selected);
		},

		"selected": function( elem ) {
			// Accessing this property makes selected-by-default
			// options in Safari work properly
			if ( elem.parentNode ) {
				elem.parentNode.selectedIndex;
			}

			return elem.selected === true;
		},

		// Contents
		"empty": function( elem ) {
			// http://www.w3.org/TR/selectors/#empty-pseudo
			// :empty is negated by element (1) or content nodes (text: 3; cdata: 4; entity ref: 5),
			//   but not by others (comment: 8; processing instruction: 7; etc.)
			// nodeType < 6 works because attributes (2) do not appear as children
			for ( elem = elem.firstChild; elem; elem = elem.nextSibling ) {
				if ( elem.nodeType < 6 ) {
					return false;
				}
			}
			return true;
		},

		"parent": function( elem ) {
			return !Expr.pseudos["empty"]( elem );
		},

		// Element/input types
		"header": function( elem ) {
			return rheader.test( elem.nodeName );
		},

		"input": function( elem ) {
			return rinputs.test( elem.nodeName );
		},

		"button": function( elem ) {
			var name = elem.nodeName.toLowerCase();
			return name === "input" && elem.type === "button" || name === "button";
		},

		"text": function( elem ) {
			var attr;
			return elem.nodeName.toLowerCase() === "input" &&
				elem.type === "text" &&

				// Support: IE<8
				// New HTML5 attribute values (e.g., "search") appear with elem.type === "text"
				( (attr = elem.getAttribute("type")) == null || attr.toLowerCase() === "text" );
		},

		// Position-in-collection
		"first": createPositionalPseudo(function() {
			return [ 0 ];
		}),

		"last": createPositionalPseudo(function( matchIndexes, length ) {
			return [ length - 1 ];
		}),

		"eq": createPositionalPseudo(function( matchIndexes, length, argument ) {
			return [ argument < 0 ? argument + length : argument ];
		}),

		"even": createPositionalPseudo(function( matchIndexes, length ) {
			var i = 0;
			for ( ; i < length; i += 2 ) {
				matchIndexes.push( i );
			}
			return matchIndexes;
		}),

		"odd": createPositionalPseudo(function( matchIndexes, length ) {
			var i = 1;
			for ( ; i < length; i += 2 ) {
				matchIndexes.push( i );
			}
			return matchIndexes;
		}),

		"lt": createPositionalPseudo(function( matchIndexes, length, argument ) {
			var i = argument < 0 ? argument + length : argument;
			for ( ; --i >= 0; ) {
				matchIndexes.push( i );
			}
			return matchIndexes;
		}),

		"gt": createPositionalPseudo(function( matchIndexes, length, argument ) {
			var i = argument < 0 ? argument + length : argument;
			for ( ; ++i < length; ) {
				matchIndexes.push( i );
			}
			return matchIndexes;
		})
	}
};

Expr.pseudos["nth"] = Expr.pseudos["eq"];

// Add button/input type pseudos
for ( i in { radio: true, checkbox: true, file: true, password: true, image: true } ) {
	Expr.pseudos[ i ] = createInputPseudo( i );
}
for ( i in { submit: true, reset: true } ) {
	Expr.pseudos[ i ] = createButtonPseudo( i );
}

// Easy API for creating new setFilters
function setFilters() {}
setFilters.prototype = Expr.filters = Expr.pseudos;
Expr.setFilters = new setFilters();

tokenize = Sizzle.tokenize = function( selector, parseOnly ) {
	var matched, match, tokens, type,
		soFar, groups, preFilters,
		cached = tokenCache[ selector + " " ];

	if ( cached ) {
		return parseOnly ? 0 : cached.slice( 0 );
	}

	soFar = selector;
	groups = [];
	preFilters = Expr.preFilter;

	while ( soFar ) {

		// Comma and first run
		if ( !matched || (match = rcomma.exec( soFar )) ) {
			if ( match ) {
				// Don't consume trailing commas as valid
				soFar = soFar.slice( match[0].length ) || soFar;
			}
			groups.push( (tokens = []) );
		}

		matched = false;

		// Combinators
		if ( (match = rcombinators.exec( soFar )) ) {
			matched = match.shift();
			tokens.push({
				value: matched,
				// Cast descendant combinators to space
				type: match[0].replace( rtrim, " " )
			});
			soFar = soFar.slice( matched.length );
		}

		// Filters
		for ( type in Expr.filter ) {
			if ( (match = matchExpr[ type ].exec( soFar )) && (!preFilters[ type ] ||
				(match = preFilters[ type ]( match ))) ) {
				matched = match.shift();
				tokens.push({
					value: matched,
					type: type,
					matches: match
				});
				soFar = soFar.slice( matched.length );
			}
		}

		if ( !matched ) {
			break;
		}
	}

	// Return the length of the invalid excess
	// if we're just parsing
	// Otherwise, throw an error or return tokens
	return parseOnly ?
		soFar.length :
		soFar ?
			Sizzle.error( selector ) :
			// Cache the tokens
			tokenCache( selector, groups ).slice( 0 );
};

function toSelector( tokens ) {
	var i = 0,
		len = tokens.length,
		selector = "";
	for ( ; i < len; i++ ) {
		selector += tokens[i].value;
	}
	return selector;
}

function addCombinator( matcher, combinator, base ) {
	var dir = combinator.dir,
		checkNonElements = base && dir === "parentNode",
		doneName = done++;

	return combinator.first ?
		// Check against closest ancestor/preceding element
		function( elem, context, xml ) {
			while ( (elem = elem[ dir ]) ) {
				if ( elem.nodeType === 1 || checkNonElements ) {
					return matcher( elem, context, xml );
				}
			}
		} :

		// Check against all ancestor/preceding elements
		function( elem, context, xml ) {
			var oldCache, outerCache,
				newCache = [ dirruns, doneName ];

			// We can't set arbitrary data on XML nodes, so they don't benefit from dir caching
			if ( xml ) {
				while ( (elem = elem[ dir ]) ) {
					if ( elem.nodeType === 1 || checkNonElements ) {
						if ( matcher( elem, context, xml ) ) {
							return true;
						}
					}
				}
			} else {
				while ( (elem = elem[ dir ]) ) {
					if ( elem.nodeType === 1 || checkNonElements ) {
						outerCache = elem[ expando ] || (elem[ expando ] = {});
						if ( (oldCache = outerCache[ dir ]) &&
							oldCache[ 0 ] === dirruns && oldCache[ 1 ] === doneName ) {

							// Assign to newCache so results back-propagate to previous elements
							return (newCache[ 2 ] = oldCache[ 2 ]);
						} else {
							// Reuse newcache so results back-propagate to previous elements
							outerCache[ dir ] = newCache;

							// A match means we're done; a fail means we have to keep checking
							if ( (newCache[ 2 ] = matcher( elem, context, xml )) ) {
								return true;
							}
						}
					}
				}
			}
		};
}

function elementMatcher( matchers ) {
	return matchers.length > 1 ?
		function( elem, context, xml ) {
			var i = matchers.length;
			while ( i-- ) {
				if ( !matchers[i]( elem, context, xml ) ) {
					return false;
				}
			}
			return true;
		} :
		matchers[0];
}

function multipleContexts( selector, contexts, results ) {
	var i = 0,
		len = contexts.length;
	for ( ; i < len; i++ ) {
		Sizzle( selector, contexts[i], results );
	}
	return results;
}

function condense( unmatched, map, filter, context, xml ) {
	var elem,
		newUnmatched = [],
		i = 0,
		len = unmatched.length,
		mapped = map != null;

	for ( ; i < len; i++ ) {
		if ( (elem = unmatched[i]) ) {
			if ( !filter || filter( elem, context, xml ) ) {
				newUnmatched.push( elem );
				if ( mapped ) {
					map.push( i );
				}
			}
		}
	}

	return newUnmatched;
}

function setMatcher( preFilter, selector, matcher, postFilter, postFinder, postSelector ) {
	if ( postFilter && !postFilter[ expando ] ) {
		postFilter = setMatcher( postFilter );
	}
	if ( postFinder && !postFinder[ expando ] ) {
		postFinder = setMatcher( postFinder, postSelector );
	}
	return markFunction(function( seed, results, context, xml ) {
		var temp, i, elem,
			preMap = [],
			postMap = [],
			preexisting = results.length,

			// Get initial elements from seed or context
			elems = seed || multipleContexts( selector || "*", context.nodeType ? [ context ] : context, [] ),

			// Prefilter to get matcher input, preserving a map for seed-results synchronization
			matcherIn = preFilter && ( seed || !selector ) ?
				condense( elems, preMap, preFilter, context, xml ) :
				elems,

			matcherOut = matcher ?
				// If we have a postFinder, or filtered seed, or non-seed postFilter or preexisting results,
				postFinder || ( seed ? preFilter : preexisting || postFilter ) ?

					// ...intermediate processing is necessary
					[] :

					// ...otherwise use results directly
					results :
				matcherIn;

		// Find primary matches
		if ( matcher ) {
			matcher( matcherIn, matcherOut, context, xml );
		}

		// Apply postFilter
		if ( postFilter ) {
			temp = condense( matcherOut, postMap );
			postFilter( temp, [], context, xml );

			// Un-match failing elements by moving them back to matcherIn
			i = temp.length;
			while ( i-- ) {
				if ( (elem = temp[i]) ) {
					matcherOut[ postMap[i] ] = !(matcherIn[ postMap[i] ] = elem);
				}
			}
		}

		if ( seed ) {
			if ( postFinder || preFilter ) {
				if ( postFinder ) {
					// Get the final matcherOut by condensing this intermediate into postFinder contexts
					temp = [];
					i = matcherOut.length;
					while ( i-- ) {
						if ( (elem = matcherOut[i]) ) {
							// Restore matcherIn since elem is not yet a final match
							temp.push( (matcherIn[i] = elem) );
						}
					}
					postFinder( null, (matcherOut = []), temp, xml );
				}

				// Move matched elements from seed to results to keep them synchronized
				i = matcherOut.length;
				while ( i-- ) {
					if ( (elem = matcherOut[i]) &&
						(temp = postFinder ? indexOf.call( seed, elem ) : preMap[i]) > -1 ) {

						seed[temp] = !(results[temp] = elem);
					}
				}
			}

		// Add elements to results, through postFinder if defined
		} else {
			matcherOut = condense(
				matcherOut === results ?
					matcherOut.splice( preexisting, matcherOut.length ) :
					matcherOut
			);
			if ( postFinder ) {
				postFinder( null, results, matcherOut, xml );
			} else {
				push.apply( results, matcherOut );
			}
		}
	});
}

function matcherFromTokens( tokens ) {
	var checkContext, matcher, j,
		len = tokens.length,
		leadingRelative = Expr.relative[ tokens[0].type ],
		implicitRelative = leadingRelative || Expr.relative[" "],
		i = leadingRelative ? 1 : 0,

		// The foundational matcher ensures that elements are reachable from top-level context(s)
		matchContext = addCombinator( function( elem ) {
			return elem === checkContext;
		}, implicitRelative, true ),
		matchAnyContext = addCombinator( function( elem ) {
			return indexOf.call( checkContext, elem ) > -1;
		}, implicitRelative, true ),
		matchers = [ function( elem, context, xml ) {
			return ( !leadingRelative && ( xml || context !== outermostContext ) ) || (
				(checkContext = context).nodeType ?
					matchContext( elem, context, xml ) :
					matchAnyContext( elem, context, xml ) );
		} ];

	for ( ; i < len; i++ ) {
		if ( (matcher = Expr.relative[ tokens[i].type ]) ) {
			matchers = [ addCombinator(elementMatcher( matchers ), matcher) ];
		} else {
			matcher = Expr.filter[ tokens[i].type ].apply( null, tokens[i].matches );

			// Return special upon seeing a positional matcher
			if ( matcher[ expando ] ) {
				// Find the next relative operator (if any) for proper handling
				j = ++i;
				for ( ; j < len; j++ ) {
					if ( Expr.relative[ tokens[j].type ] ) {
						break;
					}
				}
				return setMatcher(
					i > 1 && elementMatcher( matchers ),
					i > 1 && toSelector(
						// If the preceding token was a descendant combinator, insert an implicit any-element `*`
						tokens.slice( 0, i - 1 ).concat({ value: tokens[ i - 2 ].type === " " ? "*" : "" })
					).replace( rtrim, "$1" ),
					matcher,
					i < j && matcherFromTokens( tokens.slice( i, j ) ),
					j < len && matcherFromTokens( (tokens = tokens.slice( j )) ),
					j < len && toSelector( tokens )
				);
			}
			matchers.push( matcher );
		}
	}

	return elementMatcher( matchers );
}

function matcherFromGroupMatchers( elementMatchers, setMatchers ) {
	var bySet = setMatchers.length > 0,
		byElement = elementMatchers.length > 0,
		superMatcher = function( seed, context, xml, results, outermost ) {
			var elem, j, matcher,
				matchedCount = 0,
				i = "0",
				unmatched = seed && [],
				setMatched = [],
				contextBackup = outermostContext,
				// We must always have either seed elements or outermost context
				elems = seed || byElement && Expr.find["TAG"]( "*", outermost ),
				// Use integer dirruns iff this is the outermost matcher
				dirrunsUnique = (dirruns += contextBackup == null ? 1 : Math.random() || 0.1),
				len = elems.length;

			if ( outermost ) {
				outermostContext = context !== document && context;
			}

			// Add elements passing elementMatchers directly to results
			// Keep `i` a string if there are no elements so `matchedCount` will be "00" below
			// Support: IE<9, Safari
			// Tolerate NodeList properties (IE: "length"; Safari: <number>) matching elements by id
			for ( ; i !== len && (elem = elems[i]) != null; i++ ) {
				if ( byElement && elem ) {
					j = 0;
					while ( (matcher = elementMatchers[j++]) ) {
						if ( matcher( elem, context, xml ) ) {
							results.push( elem );
							break;
						}
					}
					if ( outermost ) {
						dirruns = dirrunsUnique;
					}
				}

				// Track unmatched elements for set filters
				if ( bySet ) {
					// They will have gone through all possible matchers
					if ( (elem = !matcher && elem) ) {
						matchedCount--;
					}

					// Lengthen the array for every element, matched or not
					if ( seed ) {
						unmatched.push( elem );
					}
				}
			}

			// Apply set filters to unmatched elements
			matchedCount += i;
			if ( bySet && i !== matchedCount ) {
				j = 0;
				while ( (matcher = setMatchers[j++]) ) {
					matcher( unmatched, setMatched, context, xml );
				}

				if ( seed ) {
					// Reintegrate element matches to eliminate the need for sorting
					if ( matchedCount > 0 ) {
						while ( i-- ) {
							if ( !(unmatched[i] || setMatched[i]) ) {
								setMatched[i] = pop.call( results );
							}
						}
					}

					// Discard index placeholder values to get only actual matches
					setMatched = condense( setMatched );
				}

				// Add matches to results
				push.apply( results, setMatched );

				// Seedless set matches succeeding multiple successful matchers stipulate sorting
				if ( outermost && !seed && setMatched.length > 0 &&
					( matchedCount + setMatchers.length ) > 1 ) {

					Sizzle.uniqueSort( results );
				}
			}

			// Override manipulation of globals by nested matchers
			if ( outermost ) {
				dirruns = dirrunsUnique;
				outermostContext = contextBackup;
			}

			return unmatched;
		};

	return bySet ?
		markFunction( superMatcher ) :
		superMatcher;
}

compile = Sizzle.compile = function( selector, match /* Internal Use Only */ ) {
	var i,
		setMatchers = [],
		elementMatchers = [],
		cached = compilerCache[ selector + " " ];

	if ( !cached ) {
		// Generate a function of recursive functions that can be used to check each element
		if ( !match ) {
			match = tokenize( selector );
		}
		i = match.length;
		while ( i-- ) {
			cached = matcherFromTokens( match[i] );
			if ( cached[ expando ] ) {
				setMatchers.push( cached );
			} else {
				elementMatchers.push( cached );
			}
		}

		// Cache the compiled function
		cached = compilerCache( selector, matcherFromGroupMatchers( elementMatchers, setMatchers ) );

		// Save selector and tokenization
		cached.selector = selector;
	}
	return cached;
};

/**
 * A low-level selection function that works with Sizzle's compiled
 *  selector functions
 * @param {String|Function} selector A selector or a pre-compiled
 *  selector function built with Sizzle.compile
 * @param {Element} context
 * @param {Array} [results]
 * @param {Array} [seed] A set of elements to match against
 */
select = Sizzle.select = function( selector, context, results, seed ) {
	var i, tokens, token, type, find,
		compiled = typeof selector === "function" && selector,
		match = !seed && tokenize( (selector = compiled.selector || selector) );

	results = results || [];

	// Try to minimize operations if there is no seed and only one group
	if ( match.length === 1 ) {

		// Take a shortcut and set the context if the root selector is an ID
		tokens = match[0] = match[0].slice( 0 );
		if ( tokens.length > 2 && (token = tokens[0]).type === "ID" &&
				support.getById && context.nodeType === 9 && documentIsHTML &&
				Expr.relative[ tokens[1].type ] ) {

			context = ( Expr.find["ID"]( token.matches[0].replace(runescape, funescape), context ) || [] )[0];
			if ( !context ) {
				return results;

			// Precompiled matchers will still verify ancestry, so step up a level
			} else if ( compiled ) {
				context = context.parentNode;
			}

			selector = selector.slice( tokens.shift().value.length );
		}

		// Fetch a seed set for right-to-left matching
		i = matchExpr["needsContext"].test( selector ) ? 0 : tokens.length;
		while ( i-- ) {
			token = tokens[i];

			// Abort if we hit a combinator
			if ( Expr.relative[ (type = token.type) ] ) {
				break;
			}
			if ( (find = Expr.find[ type ]) ) {
				// Search, expanding context for leading sibling combinators
				if ( (seed = find(
					token.matches[0].replace( runescape, funescape ),
					rsibling.test( tokens[0].type ) && testContext( context.parentNode ) || context
				)) ) {

					// If seed is empty or no tokens remain, we can return early
					tokens.splice( i, 1 );
					selector = seed.length && toSelector( tokens );
					if ( !selector ) {
						push.apply( results, seed );
						return results;
					}

					break;
				}
			}
		}
	}

	// Compile and execute a filtering function if one is not provided
	// Provide `match` to avoid retokenization if we modified the selector above
	( compiled || compile( selector, match ) )(
		seed,
		context,
		!documentIsHTML,
		results,
		rsibling.test( selector ) && testContext( context.parentNode ) || context
	);
	return results;
};

// One-time assignments

// Sort stability
support.sortStable = expando.split("").sort( sortOrder ).join("") === expando;

// Support: Chrome<14
// Always assume duplicates if they aren't passed to the comparison function
support.detectDuplicates = !!hasDuplicate;

// Initialize against the default document
setDocument();

// Support: Webkit<537.32 - Safari 6.0.3/Chrome 25 (fixed in Chrome 27)
// Detached nodes confoundingly follow *each other*
support.sortDetached = assert(function( div1 ) {
	// Should return 1, but returns 4 (following)
	return div1.compareDocumentPosition( document.createElement("div") ) & 1;
});

// Support: IE<8
// Prevent attribute/property "interpolation"
// http://msdn.microsoft.com/en-us/library/ms536429%28VS.85%29.aspx
if ( !assert(function( div ) {
	div.innerHTML = "<a href='#'></a>";
	return div.firstChild.getAttribute("href") === "#" ;
}) ) {
	addHandle( "type|href|height|width", function( elem, name, isXML ) {
		if ( !isXML ) {
			return elem.getAttribute( name, name.toLowerCase() === "type" ? 1 : 2 );
		}
	});
}

// Support: IE<9
// Use defaultValue in place of getAttribute("value")
if ( !support.attributes || !assert(function( div ) {
	div.innerHTML = "<input/>";
	div.firstChild.setAttribute( "value", "" );
	return div.firstChild.getAttribute( "value" ) === "";
}) ) {
	addHandle( "value", function( elem, name, isXML ) {
		if ( !isXML && elem.nodeName.toLowerCase() === "input" ) {
			return elem.defaultValue;
		}
	});
}

// Support: IE<9
// Use getAttributeNode to fetch booleans when getAttribute lies
if ( !assert(function( div ) {
	return div.getAttribute("disabled") == null;
}) ) {
	addHandle( booleans, function( elem, name, isXML ) {
		var val;
		if ( !isXML ) {
			return elem[ name ] === true ? name.toLowerCase() :
					(val = elem.getAttributeNode( name )) && val.specified ?
					val.value :
				null;
		}
	});
}

return Sizzle;

})( window );



jQuery.find = Sizzle;
jQuery.expr = Sizzle.selectors;
jQuery.expr[":"] = jQuery.expr.pseudos;
jQuery.unique = Sizzle.uniqueSort;
jQuery.text = Sizzle.getText;
jQuery.isXMLDoc = Sizzle.isXML;
jQuery.contains = Sizzle.contains;



var rneedsContext = jQuery.expr.match.needsContext;

var rsingleTag = (/^<(\w+)\s*\/?>(?:<\/\1>|)$/);



var risSimple = /^.[^:#\[\.,]*$/;

// Implement the identical functionality for filter and not
function winnow( elements, qualifier, not ) {
	if ( jQuery.isFunction( qualifier ) ) {
		return jQuery.grep( elements, function( elem, i ) {
			/* jshint -W018 */
			return !!qualifier.call( elem, i, elem ) !== not;
		});

	}

	if ( qualifier.nodeType ) {
		return jQuery.grep( elements, function( elem ) {
			return ( elem === qualifier ) !== not;
		});

	}

	if ( typeof qualifier === "string" ) {
		if ( risSimple.test( qualifier ) ) {
			return jQuery.filter( qualifier, elements, not );
		}

		qualifier = jQuery.filter( qualifier, elements );
	}

	return jQuery.grep( elements, function( elem ) {
		return ( indexOf.call( qualifier, elem ) >= 0 ) !== not;
	});
}

jQuery.filter = function( expr, elems, not ) {
	var elem = elems[ 0 ];

	if ( not ) {
		expr = ":not(" + expr + ")";
	}

	return elems.length === 1 && elem.nodeType === 1 ?
		jQuery.find.matchesSelector( elem, expr ) ? [ elem ] : [] :
		jQuery.find.matches( expr, jQuery.grep( elems, function( elem ) {
			return elem.nodeType === 1;
		}));
};

jQuery.fn.extend({
	find: function( selector ) {
		var i,
			len = this.length,
			ret = [],
			self = this;

		if ( typeof selector !== "string" ) {
			return this.pushStack( jQuery( selector ).filter(function() {
				for ( i = 0; i < len; i++ ) {
					if ( jQuery.contains( self[ i ], this ) ) {
						return true;
					}
				}
			}) );
		}

		for ( i = 0; i < len; i++ ) {
			jQuery.find( selector, self[ i ], ret );
		}

		// Needed because $( selector, context ) becomes $( context ).find( selector )
		ret = this.pushStack( len > 1 ? jQuery.unique( ret ) : ret );
		ret.selector = this.selector ? this.selector + " " + selector : selector;
		return ret;
	},
	filter: function( selector ) {
		return this.pushStack( winnow(this, selector || [], false) );
	},
	not: function( selector ) {
		return this.pushStack( winnow(this, selector || [], true) );
	},
	is: function( selector ) {
		return !!winnow(
			this,

			// If this is a positional/relative selector, check membership in the returned set
			// so $("p:first").is("p:last") won't return true for a doc with two "p".
			typeof selector === "string" && rneedsContext.test( selector ) ?
				jQuery( selector ) :
				selector || [],
			false
		).length;
	}
});


// Initialize a jQuery object


// A central reference to the root jQuery(document)
var rootjQuery,

	// A simple way to check for HTML strings
	// Prioritize #id over <tag> to avoid XSS via location.hash (#9521)
	// Strict HTML recognition (#11290: must start with <)
	rquickExpr = /^(?:\s*(<[\w\W]+>)[^>]*|#([\w-]*))$/,

	init = jQuery.fn.init = function( selector, context ) {
		var match, elem;

		// HANDLE: $(""), $(null), $(undefined), $(false)
		if ( !selector ) {
			return this;
		}

		// Handle HTML strings
		if ( typeof selector === "string" ) {
			if ( selector[0] === "<" && selector[ selector.length - 1 ] === ">" && selector.length >= 3 ) {
				// Assume that strings that start and end with <> are HTML and skip the regex check
				match = [ null, selector, null ];

			} else {
				match = rquickExpr.exec( selector );
			}

			// Match html or make sure no context is specified for #id
			if ( match && (match[1] || !context) ) {

				// HANDLE: $(html) -> $(array)
				if ( match[1] ) {
					context = context instanceof jQuery ? context[0] : context;

					// scripts is true for back-compat
					// Intentionally let the error be thrown if parseHTML is not present
					jQuery.merge( this, jQuery.parseHTML(
						match[1],
						context && context.nodeType ? context.ownerDocument || context : document,
						true
					) );

					// HANDLE: $(html, props)
					if ( rsingleTag.test( match[1] ) && jQuery.isPlainObject( context ) ) {
						for ( match in context ) {
							// Properties of context are called as methods if possible
							if ( jQuery.isFunction( this[ match ] ) ) {
								this[ match ]( context[ match ] );

							// ...and otherwise set as attributes
							} else {
								this.attr( match, context[ match ] );
							}
						}
					}

					return this;

				// HANDLE: $(#id)
				} else {
					elem = document.getElementById( match[2] );

					// Check parentNode to catch when Blackberry 4.6 returns
					// nodes that are no longer in the document #6963
					if ( elem && elem.parentNode ) {
						// Inject the element directly into the jQuery object
						this.length = 1;
						this[0] = elem;
					}

					this.context = document;
					this.selector = selector;
					return this;
				}

			// HANDLE: $(expr, $(...))
			} else if ( !context || context.jquery ) {
				return ( context || rootjQuery ).find( selector );

			// HANDLE: $(expr, context)
			// (which is just equivalent to: $(context).find(expr)
			} else {
				return this.constructor( context ).find( selector );
			}

		// HANDLE: $(DOMElement)
		} else if ( selector.nodeType ) {
			this.context = this[0] = selector;
			this.length = 1;
			return this;

		// HANDLE: $(function)
		// Shortcut for document ready
		} else if ( jQuery.isFunction( selector ) ) {
			return typeof rootjQuery.ready !== "undefined" ?
				rootjQuery.ready( selector ) :
				// Execute immediately if ready is not present
				selector( jQuery );
		}

		if ( selector.selector !== undefined ) {
			this.selector = selector.selector;
			this.context = selector.context;
		}

		return jQuery.makeArray( selector, this );
	};

// Give the init function the jQuery prototype for later instantiation
init.prototype = jQuery.fn;

// Initialize central reference
rootjQuery = jQuery( document );


var rparentsprev = /^(?:parents|prev(?:Until|All))/,
	// methods guaranteed to produce a unique set when starting from a unique set
	guaranteedUnique = {
		children: true,
		contents: true,
		next: true,
		prev: true
	};

jQuery.extend({
	dir: function( elem, dir, until ) {
		var matched = [],
			truncate = until !== undefined;

		while ( (elem = elem[ dir ]) && elem.nodeType !== 9 ) {
			if ( elem.nodeType === 1 ) {
				if ( truncate && jQuery( elem ).is( until ) ) {
					break;
				}
				matched.push( elem );
			}
		}
		return matched;
	},

	sibling: function( n, elem ) {
		var matched = [];

		for ( ; n; n = n.nextSibling ) {
			if ( n.nodeType === 1 && n !== elem ) {
				matched.push( n );
			}
		}

		return matched;
	}
});

jQuery.fn.extend({
	has: function( target ) {
		var targets = jQuery( target, this ),
			l = targets.length;

		return this.filter(function() {
			var i = 0;
			for ( ; i < l; i++ ) {
				if ( jQuery.contains( this, targets[i] ) ) {
					return true;
				}
			}
		});
	},

	closest: function( selectors, context ) {
		var cur,
			i = 0,
			l = this.length,
			matched = [],
			pos = rneedsContext.test( selectors ) || typeof selectors !== "string" ?
				jQuery( selectors, context || this.context ) :
				0;

		for ( ; i < l; i++ ) {
			for ( cur = this[i]; cur && cur !== context; cur = cur.parentNode ) {
				// Always skip document fragments
				if ( cur.nodeType < 11 && (pos ?
					pos.index(cur) > -1 :

					// Don't pass non-elements to Sizzle
					cur.nodeType === 1 &&
						jQuery.find.matchesSelector(cur, selectors)) ) {

					matched.push( cur );
					break;
				}
			}
		}

		return this.pushStack( matched.length > 1 ? jQuery.unique( matched ) : matched );
	},

	// Determine the position of an element within
	// the matched set of elements
	index: function( elem ) {

		// No argument, return index in parent
		if ( !elem ) {
			return ( this[ 0 ] && this[ 0 ].parentNode ) ? this.first().prevAll().length : -1;
		}

		// index in selector
		if ( typeof elem === "string" ) {
			return indexOf.call( jQuery( elem ), this[ 0 ] );
		}

		// Locate the position of the desired element
		return indexOf.call( this,

			// If it receives a jQuery object, the first element is used
			elem.jquery ? elem[ 0 ] : elem
		);
	},

	add: function( selector, context ) {
		return this.pushStack(
			jQuery.unique(
				jQuery.merge( this.get(), jQuery( selector, context ) )
			)
		);
	},

	addBack: function( selector ) {
		return this.add( selector == null ?
			this.prevObject : this.prevObject.filter(selector)
		);
	}
});

function sibling( cur, dir ) {
	while ( (cur = cur[dir]) && cur.nodeType !== 1 ) {}
	return cur;
}

jQuery.each({
	parent: function( elem ) {
		var parent = elem.parentNode;
		return parent && parent.nodeType !== 11 ? parent : null;
	},
	parents: function( elem ) {
		return jQuery.dir( elem, "parentNode" );
	},
	parentsUntil: function( elem, i, until ) {
		return jQuery.dir( elem, "parentNode", until );
	},
	next: function( elem ) {
		return sibling( elem, "nextSibling" );
	},
	prev: function( elem ) {
		return sibling( elem, "previousSibling" );
	},
	nextAll: function( elem ) {
		return jQuery.dir( elem, "nextSibling" );
	},
	prevAll: function( elem ) {
		return jQuery.dir( elem, "previousSibling" );
	},
	nextUntil: function( elem, i, until ) {
		return jQuery.dir( elem, "nextSibling", until );
	},
	prevUntil: function( elem, i, until ) {
		return jQuery.dir( elem, "previousSibling", until );
	},
	siblings: function( elem ) {
		return jQuery.sibling( ( elem.parentNode || {} ).firstChild, elem );
	},
	children: function( elem ) {
		return jQuery.sibling( elem.firstChild );
	},
	contents: function( elem ) {
		return elem.contentDocument || jQuery.merge( [], elem.childNodes );
	}
}, function( name, fn ) {
	jQuery.fn[ name ] = function( until, selector ) {
		var matched = jQuery.map( this, fn, until );

		if ( name.slice( -5 ) !== "Until" ) {
			selector = until;
		}

		if ( selector && typeof selector === "string" ) {
			matched = jQuery.filter( selector, matched );
		}

		if ( this.length > 1 ) {
			// Remove duplicates
			if ( !guaranteedUnique[ name ] ) {
				jQuery.unique( matched );
			}

			// Reverse order for parents* and prev-derivatives
			if ( rparentsprev.test( name ) ) {
				matched.reverse();
			}
		}

		return this.pushStack( matched );
	};
});
var rnotwhite = (/\S+/g);



// String to Object options format cache
var optionsCache = {};

// Convert String-formatted options into Object-formatted ones and store in cache
function createOptions( options ) {
	var object = optionsCache[ options ] = {};
	jQuery.each( options.match( rnotwhite ) || [], function( _, flag ) {
		object[ flag ] = true;
	});
	return object;
}

/*
 * Create a callback list using the following parameters:
 *
 *	options: an optional list of space-separated options that will change how
 *			the callback list behaves or a more traditional option object
 *
 * By default a callback list will act like an event callback list and can be
 * "fired" multiple times.
 *
 * Possible options:
 *
 *	once:			will ensure the callback list can only be fired once (like a Deferred)
 *
 *	memory:			will keep track of previous values and will call any callback added
 *					after the list has been fired right away with the latest "memorized"
 *					values (like a Deferred)
 *
 *	unique:			will ensure a callback can only be added once (no duplicate in the list)
 *
 *	stopOnFalse:	interrupt callings when a callback returns false
 *
 */
jQuery.Callbacks = function( options ) {

	// Convert options from String-formatted to Object-formatted if needed
	// (we check in cache first)
	options = typeof options === "string" ?
		( optionsCache[ options ] || createOptions( options ) ) :
		jQuery.extend( {}, options );

	var // Last fire value (for non-forgettable lists)
		memory,
		// Flag to know if list was already fired
		fired,
		// Flag to know if list is currently firing
		firing,
		// First callback to fire (used internally by add and fireWith)
		firingStart,
		// End of the loop when firing
		firingLength,
		// Index of currently firing callback (modified by remove if needed)
		firingIndex,
		// Actual callback list
		list = [],
		// Stack of fire calls for repeatable lists
		stack = !options.once && [],
		// Fire callbacks
		fire = function( data ) {
			memory = options.memory && data;
			fired = true;
			firingIndex = firingStart || 0;
			firingStart = 0;
			firingLength = list.length;
			firing = true;
			for ( ; list && firingIndex < firingLength; firingIndex++ ) {
				if ( list[ firingIndex ].apply( data[ 0 ], data[ 1 ] ) === false && options.stopOnFalse ) {
					memory = false; // To prevent further calls using add
					break;
				}
			}
			firing = false;
			if ( list ) {
				if ( stack ) {
					if ( stack.length ) {
						fire( stack.shift() );
					}
				} else if ( memory ) {
					list = [];
				} else {
					self.disable();
				}
			}
		},
		// Actual Callbacks object
		self = {
			// Add a callback or a collection of callbacks to the list
			add: function() {
				if ( list ) {
					// First, we save the current length
					var start = list.length;
					(function add( args ) {
						jQuery.each( args, function( _, arg ) {
							var type = jQuery.type( arg );
							if ( type === "function" ) {
								if ( !options.unique || !self.has( arg ) ) {
									list.push( arg );
								}
							} else if ( arg && arg.length && type !== "string" ) {
								// Inspect recursively
								add( arg );
							}
						});
					})( arguments );
					// Do we need to add the callbacks to the
					// current firing batch?
					if ( firing ) {
						firingLength = list.length;
					// With memory, if we're not firing then
					// we should call right away
					} else if ( memory ) {
						firingStart = start;
						fire( memory );
					}
				}
				return this;
			},
			// Remove a callback from the list
			remove: function() {
				if ( list ) {
					jQuery.each( arguments, function( _, arg ) {
						var index;
						while ( ( index = jQuery.inArray( arg, list, index ) ) > -1 ) {
							list.splice( index, 1 );
							// Handle firing indexes
							if ( firing ) {
								if ( index <= firingLength ) {
									firingLength--;
								}
								if ( index <= firingIndex ) {
									firingIndex--;
								}
							}
						}
					});
				}
				return this;
			},
			// Check if a given callback is in the list.
			// If no argument is given, return whether or not list has callbacks attached.
			has: function( fn ) {
				return fn ? jQuery.inArray( fn, list ) > -1 : !!( list && list.length );
			},
			// Remove all callbacks from the list
			empty: function() {
				list = [];
				firingLength = 0;
				return this;
			},
			// Have the list do nothing anymore
			disable: function() {
				list = stack = memory = undefined;
				return this;
			},
			// Is it disabled?
			disabled: function() {
				return !list;
			},
			// Lock the list in its current state
			lock: function() {
				stack = undefined;
				if ( !memory ) {
					self.disable();
				}
				return this;
			},
			// Is it locked?
			locked: function() {
				return !stack;
			},
			// Call all callbacks with the given context and arguments
			fireWith: function( context, args ) {
				if ( list && ( !fired || stack ) ) {
					args = args || [];
					args = [ context, args.slice ? args.slice() : args ];
					if ( firing ) {
						stack.push( args );
					} else {
						fire( args );
					}
				}
				return this;
			},
			// Call all the callbacks with the given arguments
			fire: function() {
				self.fireWith( this, arguments );
				return this;
			},
			// To know if the callbacks have already been called at least once
			fired: function() {
				return !!fired;
			}
		};

	return self;
};


jQuery.extend({

	Deferred: function( func ) {
		var tuples = [
				// action, add listener, listener list, final state
				[ "resolve", "done", jQuery.Callbacks("once memory"), "resolved" ],
				[ "reject", "fail", jQuery.Callbacks("once memory"), "rejected" ],
				[ "notify", "progress", jQuery.Callbacks("memory") ]
			],
			state = "pending",
			promise = {
				state: function() {
					return state;
				},
				always: function() {
					deferred.done( arguments ).fail( arguments );
					return this;
				},
				then: function( /* fnDone, fnFail, fnProgress */ ) {
					var fns = arguments;
					return jQuery.Deferred(function( newDefer ) {
						jQuery.each( tuples, function( i, tuple ) {
							var fn = jQuery.isFunction( fns[ i ] ) && fns[ i ];
							// deferred[ done | fail | progress ] for forwarding actions to newDefer
							deferred[ tuple[1] ](function() {
								var returned = fn && fn.apply( this, arguments );
								if ( returned && jQuery.isFunction( returned.promise ) ) {
									returned.promise()
										.done( newDefer.resolve )
										.fail( newDefer.reject )
										.progress( newDefer.notify );
								} else {
									newDefer[ tuple[ 0 ] + "With" ]( this === promise ? newDefer.promise() : this, fn ? [ returned ] : arguments );
								}
							});
						});
						fns = null;
					}).promise();
				},
				// Get a promise for this deferred
				// If obj is provided, the promise aspect is added to the object
				promise: function( obj ) {
					return obj != null ? jQuery.extend( obj, promise ) : promise;
				}
			},
			deferred = {};

		// Keep pipe for back-compat
		promise.pipe = promise.then;

		// Add list-specific methods
		jQuery.each( tuples, function( i, tuple ) {
			var list = tuple[ 2 ],
				stateString = tuple[ 3 ];

			// promise[ done | fail | progress ] = list.add
			promise[ tuple[1] ] = list.add;

			// Handle state
			if ( stateString ) {
				list.add(function() {
					// state = [ resolved | rejected ]
					state = stateString;

				// [ reject_list | resolve_list ].disable; progress_list.lock
				}, tuples[ i ^ 1 ][ 2 ].disable, tuples[ 2 ][ 2 ].lock );
			}

			// deferred[ resolve | reject | notify ]
			deferred[ tuple[0] ] = function() {
				deferred[ tuple[0] + "With" ]( this === deferred ? promise : this, arguments );
				return this;
			};
			deferred[ tuple[0] + "With" ] = list.fireWith;
		});

		// Make the deferred a promise
		promise.promise( deferred );

		// Call given func if any
		if ( func ) {
			func.call( deferred, deferred );
		}

		// All done!
		return deferred;
	},

	// Deferred helper
	when: function( subordinate /* , ..., subordinateN */ ) {
		var i = 0,
			resolveValues = slice.call( arguments ),
			length = resolveValues.length,

			// the count of uncompleted subordinates
			remaining = length !== 1 || ( subordinate && jQuery.isFunction( subordinate.promise ) ) ? length : 0,

			// the master Deferred. If resolveValues consist of only a single Deferred, just use that.
			deferred = remaining === 1 ? subordinate : jQuery.Deferred(),

			// Update function for both resolve and progress values
			updateFunc = function( i, contexts, values ) {
				return function( value ) {
					contexts[ i ] = this;
					values[ i ] = arguments.length > 1 ? slice.call( arguments ) : value;
					if ( values === progressValues ) {
						deferred.notifyWith( contexts, values );
					} else if ( !( --remaining ) ) {
						deferred.resolveWith( contexts, values );
					}
				};
			},

			progressValues, progressContexts, resolveContexts;

		// add listeners to Deferred subordinates; treat others as resolved
		if ( length > 1 ) {
			progressValues = new Array( length );
			progressContexts = new Array( length );
			resolveContexts = new Array( length );
			for ( ; i < length; i++ ) {
				if ( resolveValues[ i ] && jQuery.isFunction( resolveValues[ i ].promise ) ) {
					resolveValues[ i ].promise()
						.done( updateFunc( i, resolveContexts, resolveValues ) )
						.fail( deferred.reject )
						.progress( updateFunc( i, progressContexts, progressValues ) );
				} else {
					--remaining;
				}
			}
		}

		// if we're not waiting on anything, resolve the master
		if ( !remaining ) {
			deferred.resolveWith( resolveContexts, resolveValues );
		}

		return deferred.promise();
	}
});


// The deferred used on DOM ready
var readyList;

jQuery.fn.ready = function( fn ) {
	// Add the callback
	jQuery.ready.promise().done( fn );

	return this;
};

jQuery.extend({
	// Is the DOM ready to be used? Set to true once it occurs.
	isReady: false,

	// A counter to track how many items to wait for before
	// the ready event fires. See #6781
	readyWait: 1,

	// Hold (or release) the ready event
	holdReady: function( hold ) {
		if ( hold ) {
			jQuery.readyWait++;
		} else {
			jQuery.ready( true );
		}
	},

	// Handle when the DOM is ready
	ready: function( wait ) {

		// Abort if there are pending holds or we're already ready
		if ( wait === true ? --jQuery.readyWait : jQuery.isReady ) {
			return;
		}

		// Remember that the DOM is ready
		jQuery.isReady = true;

		// If a normal DOM Ready event fired, decrement, and wait if need be
		if ( wait !== true && --jQuery.readyWait > 0 ) {
			return;
		}

		// If there are functions bound, to execute
		readyList.resolveWith( document, [ jQuery ] );

		// Trigger any bound ready events
		if ( jQuery.fn.triggerHandler ) {
			jQuery( document ).triggerHandler( "ready" );
			jQuery( document ).off( "ready" );
		}
	}
});

/**
 * The ready event handler and self cleanup method
 */
function completed() {
	document.removeEventListener( "DOMContentLoaded", completed, false );
	window.removeEventListener( "load", completed, false );
	jQuery.ready();
}

jQuery.ready.promise = function( obj ) {
	if ( !readyList ) {

		readyList = jQuery.Deferred();

		// Catch cases where $(document).ready() is called after the browser event has already occurred.
		// we once tried to use readyState "interactive" here, but it caused issues like the one
		// discovered by ChrisS here: http://bugs.jquery.com/ticket/12282#comment:15
		if ( document.readyState === "complete" ) {
			// Handle it asynchronously to allow scripts the opportunity to delay ready
			setTimeout( jQuery.ready );

		} else {

			// Use the handy event callback
			document.addEventListener( "DOMContentLoaded", completed, false );

			// A fallback to window.onload, that will always work
			window.addEventListener( "load", completed, false );
		}
	}
	return readyList.promise( obj );
};

// Kick off the DOM ready check even if the user does not
jQuery.ready.promise();




// Multifunctional method to get and set values of a collection
// The value/s can optionally be executed if it's a function
var access = jQuery.access = function( elems, fn, key, value, chainable, emptyGet, raw ) {
	var i = 0,
		len = elems.length,
		bulk = key == null;

	// Sets many values
	if ( jQuery.type( key ) === "object" ) {
		chainable = true;
		for ( i in key ) {
			jQuery.access( elems, fn, i, key[i], true, emptyGet, raw );
		}

	// Sets one value
	} else if ( value !== undefined ) {
		chainable = true;

		if ( !jQuery.isFunction( value ) ) {
			raw = true;
		}

		if ( bulk ) {
			// Bulk operations run against the entire set
			if ( raw ) {
				fn.call( elems, value );
				fn = null;

			// ...except when executing function values
			} else {
				bulk = fn;
				fn = function( elem, key, value ) {
					return bulk.call( jQuery( elem ), value );
				};
			}
		}

		if ( fn ) {
			for ( ; i < len; i++ ) {
				fn( elems[i], key, raw ? value : value.call( elems[i], i, fn( elems[i], key ) ) );
			}
		}
	}

	return chainable ?
		elems :

		// Gets
		bulk ?
			fn.call( elems ) :
			len ? fn( elems[0], key ) : emptyGet;
};


/**
 * Determines whether an object can have data
 */
jQuery.acceptData = function( owner ) {
	// Accepts only:
	//  - Node
	//    - Node.ELEMENT_NODE
	//    - Node.DOCUMENT_NODE
	//  - Object
	//    - Any
	/* jshint -W018 */
	return owner.nodeType === 1 || owner.nodeType === 9 || !( +owner.nodeType );
};


function Data() {
	// Support: Android < 4,
	// Old WebKit does not have Object.preventExtensions/freeze method,
	// return new empty object instead with no [[set]] accessor
	Object.defineProperty( this.cache = {}, 0, {
		get: function() {
			return {};
		}
	});

	this.expando = jQuery.expando + Math.random();
}

Data.uid = 1;
Data.accepts = jQuery.acceptData;

Data.prototype = {
	key: function( owner ) {
		// We can accept data for non-element nodes in modern browsers,
		// but we should not, see #8335.
		// Always return the key for a frozen object.
		if ( !Data.accepts( owner ) ) {
			return 0;
		}

		var descriptor = {},
			// Check if the owner object already has a cache key
			unlock = owner[ this.expando ];

		// If not, create one
		if ( !unlock ) {
			unlock = Data.uid++;

			// Secure it in a non-enumerable, non-writable property
			try {
				descriptor[ this.expando ] = { value: unlock };
				Object.defineProperties( owner, descriptor );

			// Support: Android < 4
			// Fallback to a less secure definition
			} catch ( e ) {
				descriptor[ this.expando ] = unlock;
				jQuery.extend( owner, descriptor );
			}
		}

		// Ensure the cache object
		if ( !this.cache[ unlock ] ) {
			this.cache[ unlock ] = {};
		}

		return unlock;
	},
	set: function( owner, data, value ) {
		var prop,
			// There may be an unlock assigned to this node,
			// if there is no entry for this "owner", create one inline
			// and set the unlock as though an owner entry had always existed
			unlock = this.key( owner ),
			cache = this.cache[ unlock ];

		// Handle: [ owner, key, value ] args
		if ( typeof data === "string" ) {
			cache[ data ] = value;

		// Handle: [ owner, { properties } ] args
		} else {
			// Fresh assignments by object are shallow copied
			if ( jQuery.isEmptyObject( cache ) ) {
				jQuery.extend( this.cache[ unlock ], data );
			// Otherwise, copy the properties one-by-one to the cache object
			} else {
				for ( prop in data ) {
					cache[ prop ] = data[ prop ];
				}
			}
		}
		return cache;
	},
	get: function( owner, key ) {
		// Either a valid cache is found, or will be created.
		// New caches will be created and the unlock returned,
		// allowing direct access to the newly created
		// empty data object. A valid owner object must be provided.
		var cache = this.cache[ this.key( owner ) ];

		return key === undefined ?
			cache : cache[ key ];
	},
	access: function( owner, key, value ) {
		var stored;
		// In cases where either:
		//
		//   1. No key was specified
		//   2. A string key was specified, but no value provided
		//
		// Take the "read" path and allow the get method to determine
		// which value to return, respectively either:
		//
		//   1. The entire cache object
		//   2. The data stored at the key
		//
		if ( key === undefined ||
				((key && typeof key === "string") && value === undefined) ) {

			stored = this.get( owner, key );

			return stored !== undefined ?
				stored : this.get( owner, jQuery.camelCase(key) );
		}

		// [*]When the key is not a string, or both a key and value
		// are specified, set or extend (existing objects) with either:
		//
		//   1. An object of properties
		//   2. A key and value
		//
		this.set( owner, key, value );

		// Since the "set" path can have two possible entry points
		// return the expected data based on which path was taken[*]
		return value !== undefined ? value : key;
	},
	remove: function( owner, key ) {
		var i, name, camel,
			unlock = this.key( owner ),
			cache = this.cache[ unlock ];

		if ( key === undefined ) {
			this.cache[ unlock ] = {};

		} else {
			// Support array or space separated string of keys
			if ( jQuery.isArray( key ) ) {
				// If "name" is an array of keys...
				// When data is initially created, via ("key", "val") signature,
				// keys will be converted to camelCase.
				// Since there is no way to tell _how_ a key was added, remove
				// both plain key and camelCase key. #12786
				// This will only penalize the array argument path.
				name = key.concat( key.map( jQuery.camelCase ) );
			} else {
				camel = jQuery.camelCase( key );
				// Try the string as a key before any manipulation
				if ( key in cache ) {
					name = [ key, camel ];
				} else {
					// If a key with the spaces exists, use it.
					// Otherwise, create an array by matching non-whitespace
					name = camel;
					name = name in cache ?
						[ name ] : ( name.match( rnotwhite ) || [] );
				}
			}

			i = name.length;
			while ( i-- ) {
				delete cache[ name[ i ] ];
			}
		}
	},
	hasData: function( owner ) {
		return !jQuery.isEmptyObject(
			this.cache[ owner[ this.expando ] ] || {}
		);
	},
	discard: function( owner ) {
		if ( owner[ this.expando ] ) {
			delete this.cache[ owner[ this.expando ] ];
		}
	}
};
var data_priv = new Data();

var data_user = new Data();



/*
	Implementation Summary

	1. Enforce API surface and semantic compatibility with 1.9.x branch
	2. Improve the module's maintainability by reducing the storage
		paths to a single mechanism.
	3. Use the same single mechanism to support "private" and "user" data.
	4. _Never_ expose "private" data to user code (TODO: Drop _data, _removeData)
	5. Avoid exposing implementation details on user objects (eg. expando properties)
	6. Provide a clear path for implementation upgrade to WeakMap in 2014
*/
var rbrace = /^(?:\{[\w\W]*\}|\[[\w\W]*\])$/,
	rmultiDash = /([A-Z])/g;

function dataAttr( elem, key, data ) {
	var name;

	// If nothing was found internally, try to fetch any
	// data from the HTML5 data-* attribute
	if ( data === undefined && elem.nodeType === 1 ) {
		name = "data-" + key.replace( rmultiDash, "-$1" ).toLowerCase();
		data = elem.getAttribute( name );

		if ( typeof data === "string" ) {
			try {
				data = data === "true" ? true :
					data === "false" ? false :
					data === "null" ? null :
					// Only convert to a number if it doesn't change the string
					+data + "" === data ? +data :
					rbrace.test( data ) ? jQuery.parseJSON( data ) :
					data;
			} catch( e ) {}

			// Make sure we set the data so it isn't changed later
			data_user.set( elem, key, data );
		} else {
			data = undefined;
		}
	}
	return data;
}

jQuery.extend({
	hasData: function( elem ) {
		return data_user.hasData( elem ) || data_priv.hasData( elem );
	},

	data: function( elem, name, data ) {
		return data_user.access( elem, name, data );
	},

	removeData: function( elem, name ) {
		data_user.remove( elem, name );
	},

	// TODO: Now that all calls to _data and _removeData have been replaced
	// with direct calls to data_priv methods, these can be deprecated.
	_data: function( elem, name, data ) {
		return data_priv.access( elem, name, data );
	},

	_removeData: function( elem, name ) {
		data_priv.remove( elem, name );
	}
});

jQuery.fn.extend({
	data: function( key, value ) {
		var i, name, data,
			elem = this[ 0 ],
			attrs = elem && elem.attributes;

		// Gets all values
		if ( key === undefined ) {
			if ( this.length ) {
				data = data_user.get( elem );

				if ( elem.nodeType === 1 && !data_priv.get( elem, "hasDataAttrs" ) ) {
					i = attrs.length;
					while ( i-- ) {

						// Support: IE11+
						// The attrs elements can be null (#14894)
						if ( attrs[ i ] ) {
							name = attrs[ i ].name;
							if ( name.indexOf( "data-" ) === 0 ) {
								name = jQuery.camelCase( name.slice(5) );
								dataAttr( elem, name, data[ name ] );
							}
						}
					}
					data_priv.set( elem, "hasDataAttrs", true );
				}
			}

			return data;
		}

		// Sets multiple values
		if ( typeof key === "object" ) {
			return this.each(function() {
				data_user.set( this, key );
			});
		}

		return access( this, function( value ) {
			var data,
				camelKey = jQuery.camelCase( key );

			// The calling jQuery object (element matches) is not empty
			// (and therefore has an element appears at this[ 0 ]) and the
			// `value` parameter was not undefined. An empty jQuery object
			// will result in `undefined` for elem = this[ 0 ] which will
			// throw an exception if an attempt to read a data cache is made.
			if ( elem && value === undefined ) {
				// Attempt to get data from the cache
				// with the key as-is
				data = data_user.get( elem, key );
				if ( data !== undefined ) {
					return data;
				}

				// Attempt to get data from the cache
				// with the key camelized
				data = data_user.get( elem, camelKey );
				if ( data !== undefined ) {
					return data;
				}

				// Attempt to "discover" the data in
				// HTML5 custom data-* attrs
				data = dataAttr( elem, camelKey, undefined );
				if ( data !== undefined ) {
					return data;
				}

				// We tried really hard, but the data doesn't exist.
				return;
			}

			// Set the data...
			this.each(function() {
				// First, attempt to store a copy or reference of any
				// data that might've been store with a camelCased key.
				var data = data_user.get( this, camelKey );

				// For HTML5 data-* attribute interop, we have to
				// store property names with dashes in a camelCase form.
				// This might not apply to all properties...*
				data_user.set( this, camelKey, value );

				// *... In the case of properties that might _actually_
				// have dashes, we need to also store a copy of that
				// unchanged property.
				if ( key.indexOf("-") !== -1 && data !== undefined ) {
					data_user.set( this, key, value );
				}
			});
		}, null, value, arguments.length > 1, null, true );
	},

	removeData: function( key ) {
		return this.each(function() {
			data_user.remove( this, key );
		});
	}
});


jQuery.extend({
	queue: function( elem, type, data ) {
		var queue;

		if ( elem ) {
			type = ( type || "fx" ) + "queue";
			queue = data_priv.get( elem, type );

			// Speed up dequeue by getting out quickly if this is just a lookup
			if ( data ) {
				if ( !queue || jQuery.isArray( data ) ) {
					queue = data_priv.access( elem, type, jQuery.makeArray(data) );
				} else {
					queue.push( data );
				}
			}
			return queue || [];
		}
	},

	dequeue: function( elem, type ) {
		type = type || "fx";

		var queue = jQuery.queue( elem, type ),
			startLength = queue.length,
			fn = queue.shift(),
			hooks = jQuery._queueHooks( elem, type ),
			next = function() {
				jQuery.dequeue( elem, type );
			};

		// If the fx queue is dequeued, always remove the progress sentinel
		if ( fn === "inprogress" ) {
			fn = queue.shift();
			startLength--;
		}

		if ( fn ) {

			// Add a progress sentinel to prevent the fx queue from being
			// automatically dequeued
			if ( type === "fx" ) {
				queue.unshift( "inprogress" );
			}

			// clear up the last queue stop function
			delete hooks.stop;
			fn.call( elem, next, hooks );
		}

		if ( !startLength && hooks ) {
			hooks.empty.fire();
		}
	},

	// not intended for public consumption - generates a queueHooks object, or returns the current one
	_queueHooks: function( elem, type ) {
		var key = type + "queueHooks";
		return data_priv.get( elem, key ) || data_priv.access( elem, key, {
			empty: jQuery.Callbacks("once memory").add(function() {
				data_priv.remove( elem, [ type + "queue", key ] );
			})
		});
	}
});

jQuery.fn.extend({
	queue: function( type, data ) {
		var setter = 2;

		if ( typeof type !== "string" ) {
			data = type;
			type = "fx";
			setter--;
		}

		if ( arguments.length < setter ) {
			return jQuery.queue( this[0], type );
		}

		return data === undefined ?
			this :
			this.each(function() {
				var queue = jQuery.queue( this, type, data );

				// ensure a hooks for this queue
				jQuery._queueHooks( this, type );

				if ( type === "fx" && queue[0] !== "inprogress" ) {
					jQuery.dequeue( this, type );
				}
			});
	},
	dequeue: function( type ) {
		return this.each(function() {
			jQuery.dequeue( this, type );
		});
	},
	clearQueue: function( type ) {
		return this.queue( type || "fx", [] );
	},
	// Get a promise resolved when queues of a certain type
	// are emptied (fx is the type by default)
	promise: function( type, obj ) {
		var tmp,
			count = 1,
			defer = jQuery.Deferred(),
			elements = this,
			i = this.length,
			resolve = function() {
				if ( !( --count ) ) {
					defer.resolveWith( elements, [ elements ] );
				}
			};

		if ( typeof type !== "string" ) {
			obj = type;
			type = undefined;
		}
		type = type || "fx";

		while ( i-- ) {
			tmp = data_priv.get( elements[ i ], type + "queueHooks" );
			if ( tmp && tmp.empty ) {
				count++;
				tmp.empty.add( resolve );
			}
		}
		resolve();
		return defer.promise( obj );
	}
});
var pnum = (/[+-]?(?:\d*\.|)\d+(?:[eE][+-]?\d+|)/).source;

var cssExpand = [ "Top", "Right", "Bottom", "Left" ];

var isHidden = function( elem, el ) {
		// isHidden might be called from jQuery#filter function;
		// in that case, element will be second argument
		elem = el || elem;
		return jQuery.css( elem, "display" ) === "none" || !jQuery.contains( elem.ownerDocument, elem );
	};

var rcheckableType = (/^(?:checkbox|radio)$/i);



(function() {
	var fragment = document.createDocumentFragment(),
		div = fragment.appendChild( document.createElement( "div" ) ),
		input = document.createElement( "input" );

	// #11217 - WebKit loses check when the name is after the checked attribute
	// Support: Windows Web Apps (WWA)
	// `name` and `type` need .setAttribute for WWA
	input.setAttribute( "type", "radio" );
	input.setAttribute( "checked", "checked" );
	input.setAttribute( "name", "t" );

	div.appendChild( input );

	// Support: Safari 5.1, iOS 5.1, Android 4.x, Android 2.3
	// old WebKit doesn't clone checked state correctly in fragments
	support.checkClone = div.cloneNode( true ).cloneNode( true ).lastChild.checked;

	// Make sure textarea (and checkbox) defaultValue is properly cloned
	// Support: IE9-IE11+
	div.innerHTML = "<textarea>x</textarea>";
	support.noCloneChecked = !!div.cloneNode( true ).lastChild.defaultValue;
})();
var strundefined = typeof undefined;



support.focusinBubbles = "onfocusin" in window;


var
	rkeyEvent = /^key/,
	rmouseEvent = /^(?:mouse|pointer|contextmenu)|click/,
	rfocusMorph = /^(?:focusinfocus|focusoutblur)$/,
	rtypenamespace = /^([^.]*)(?:\.(.+)|)$/;

function returnTrue() {
	return true;
}

function returnFalse() {
	return false;
}

function safeActiveElement() {
	try {
		return document.activeElement;
	} catch ( err ) { }
}

/*
 * Helper functions for managing events -- not part of the public interface.
 * Props to Dean Edwards' addEvent library for many of the ideas.
 */
jQuery.event = {

	global: {},

	add: function( elem, types, handler, data, selector ) {

		var handleObjIn, eventHandle, tmp,
			events, t, handleObj,
			special, handlers, type, namespaces, origType,
			elemData = data_priv.get( elem );

		// Don't attach events to noData or text/comment nodes (but allow plain objects)
		if ( !elemData ) {
			return;
		}

		// Caller can pass in an object of custom data in lieu of the handler
		if ( handler.handler ) {
			handleObjIn = handler;
			handler = handleObjIn.handler;
			selector = handleObjIn.selector;
		}

		// Make sure that the handler has a unique ID, used to find/remove it later
		if ( !handler.guid ) {
			handler.guid = jQuery.guid++;
		}

		// Init the element's event structure and main handler, if this is the first
		if ( !(events = elemData.events) ) {
			events = elemData.events = {};
		}
		if ( !(eventHandle = elemData.handle) ) {
			eventHandle = elemData.handle = function( e ) {
				// Discard the second event of a jQuery.event.trigger() and
				// when an event is called after a page has unloaded
				return typeof jQuery !== strundefined && jQuery.event.triggered !== e.type ?
					jQuery.event.dispatch.apply( elem, arguments ) : undefined;
			};
		}

		// Handle multiple events separated by a space
		types = ( types || "" ).match( rnotwhite ) || [ "" ];
		t = types.length;
		while ( t-- ) {
			tmp = rtypenamespace.exec( types[t] ) || [];
			type = origType = tmp[1];
			namespaces = ( tmp[2] || "" ).split( "." ).sort();

			// There *must* be a type, no attaching namespace-only handlers
			if ( !type ) {
				continue;
			}

			// If event changes its type, use the special event handlers for the changed type
			special = jQuery.event.special[ type ] || {};

			// If selector defined, determine special event api type, otherwise given type
			type = ( selector ? special.delegateType : special.bindType ) || type;

			// Update special based on newly reset type
			special = jQuery.event.special[ type ] || {};

			// handleObj is passed to all event handlers
			handleObj = jQuery.extend({
				type: type,
				origType: origType,
				data: data,
				handler: handler,
				guid: handler.guid,
				selector: selector,
				needsContext: selector && jQuery.expr.match.needsContext.test( selector ),
				namespace: namespaces.join(".")
			}, handleObjIn );

			// Init the event handler queue if we're the first
			if ( !(handlers = events[ type ]) ) {
				handlers = events[ type ] = [];
				handlers.delegateCount = 0;

				// Only use addEventListener if the special events handler returns false
				if ( !special.setup || special.setup.call( elem, data, namespaces, eventHandle ) === false ) {
					if ( elem.addEventListener ) {
						elem.addEventListener( type, eventHandle, false );
					}
				}
			}

			if ( special.add ) {
				special.add.call( elem, handleObj );

				if ( !handleObj.handler.guid ) {
					handleObj.handler.guid = handler.guid;
				}
			}

			// Add to the element's handler list, delegates in front
			if ( selector ) {
				handlers.splice( handlers.delegateCount++, 0, handleObj );
			} else {
				handlers.push( handleObj );
			}

			// Keep track of which events have ever been used, for event optimization
			jQuery.event.global[ type ] = true;
		}

	},

	// Detach an event or set of events from an element
	remove: function( elem, types, handler, selector, mappedTypes ) {

		var j, origCount, tmp,
			events, t, handleObj,
			special, handlers, type, namespaces, origType,
			elemData = data_priv.hasData( elem ) && data_priv.get( elem );

		if ( !elemData || !(events = elemData.events) ) {
			return;
		}

		// Once for each type.namespace in types; type may be omitted
		types = ( types || "" ).match( rnotwhite ) || [ "" ];
		t = types.length;
		while ( t-- ) {
			tmp = rtypenamespace.exec( types[t] ) || [];
			type = origType = tmp[1];
			namespaces = ( tmp[2] || "" ).split( "." ).sort();

			// Unbind all events (on this namespace, if provided) for the element
			if ( !type ) {
				for ( type in events ) {
					jQuery.event.remove( elem, type + types[ t ], handler, selector, true );
				}
				continue;
			}

			special = jQuery.event.special[ type ] || {};
			type = ( selector ? special.delegateType : special.bindType ) || type;
			handlers = events[ type ] || [];
			tmp = tmp[2] && new RegExp( "(^|\\.)" + namespaces.join("\\.(?:.*\\.|)") + "(\\.|$)" );

			// Remove matching events
			origCount = j = handlers.length;
			while ( j-- ) {
				handleObj = handlers[ j ];

				if ( ( mappedTypes || origType === handleObj.origType ) &&
					( !handler || handler.guid === handleObj.guid ) &&
					( !tmp || tmp.test( handleObj.namespace ) ) &&
					( !selector || selector === handleObj.selector || selector === "**" && handleObj.selector ) ) {
					handlers.splice( j, 1 );

					if ( handleObj.selector ) {
						handlers.delegateCount--;
					}
					if ( special.remove ) {
						special.remove.call( elem, handleObj );
					}
				}
			}

			// Remove generic event handler if we removed something and no more handlers exist
			// (avoids potential for endless recursion during removal of special event handlers)
			if ( origCount && !handlers.length ) {
				if ( !special.teardown || special.teardown.call( elem, namespaces, elemData.handle ) === false ) {
					jQuery.removeEvent( elem, type, elemData.handle );
				}

				delete events[ type ];
			}
		}

		// Remove the expando if it's no longer used
		if ( jQuery.isEmptyObject( events ) ) {
			delete elemData.handle;
			data_priv.remove( elem, "events" );
		}
	},

	trigger: function( event, data, elem, onlyHandlers ) {

		var i, cur, tmp, bubbleType, ontype, handle, special,
			eventPath = [ elem || document ],
			type = hasOwn.call( event, "type" ) ? event.type : event,
			namespaces = hasOwn.call( event, "namespace" ) ? event.namespace.split(".") : [];

		cur = tmp = elem = elem || document;

		// Don't do events on text and comment nodes
		if ( elem.nodeType === 3 || elem.nodeType === 8 ) {
			return;
		}

		// focus/blur morphs to focusin/out; ensure we're not firing them right now
		if ( rfocusMorph.test( type + jQuery.event.triggered ) ) {
			return;
		}

		if ( type.indexOf(".") >= 0 ) {
			// Namespaced trigger; create a regexp to match event type in handle()
			namespaces = type.split(".");
			type = namespaces.shift();
			namespaces.sort();
		}
		ontype = type.indexOf(":") < 0 && "on" + type;

		// Caller can pass in a jQuery.Event object, Object, or just an event type string
		event = event[ jQuery.expando ] ?
			event :
			new jQuery.Event( type, typeof event === "object" && event );

		// Trigger bitmask: & 1 for native handlers; & 2 for jQuery (always true)
		event.isTrigger = onlyHandlers ? 2 : 3;
		event.namespace = namespaces.join(".");
		event.namespace_re = event.namespace ?
			new RegExp( "(^|\\.)" + namespaces.join("\\.(?:.*\\.|)") + "(\\.|$)" ) :
			null;

		// Clean up the event in case it is being reused
		event.result = undefined;
		if ( !event.target ) {
			event.target = elem;
		}

		// Clone any incoming data and prepend the event, creating the handler arg list
		data = data == null ?
			[ event ] :
			jQuery.makeArray( data, [ event ] );

		// Allow special events to draw outside the lines
		special = jQuery.event.special[ type ] || {};
		if ( !onlyHandlers && special.trigger && special.trigger.apply( elem, data ) === false ) {
			return;
		}

		// Determine event propagation path in advance, per W3C events spec (#9951)
		// Bubble up to document, then to window; watch for a global ownerDocument var (#9724)
		if ( !onlyHandlers && !special.noBubble && !jQuery.isWindow( elem ) ) {

			bubbleType = special.delegateType || type;
			if ( !rfocusMorph.test( bubbleType + type ) ) {
				cur = cur.parentNode;
			}
			for ( ; cur; cur = cur.parentNode ) {
				eventPath.push( cur );
				tmp = cur;
			}

			// Only add window if we got to document (e.g., not plain obj or detached DOM)
			if ( tmp === (elem.ownerDocument || document) ) {
				eventPath.push( tmp.defaultView || tmp.parentWindow || window );
			}
		}

		// Fire handlers on the event path
		i = 0;
		while ( (cur = eventPath[i++]) && !event.isPropagationStopped() ) {

			event.type = i > 1 ?
				bubbleType :
				special.bindType || type;

			// jQuery handler
			handle = ( data_priv.get( cur, "events" ) || {} )[ event.type ] && data_priv.get( cur, "handle" );
			if ( handle ) {
				handle.apply( cur, data );
			}

			// Native handler
			handle = ontype && cur[ ontype ];
			if ( handle && handle.apply && jQuery.acceptData( cur ) ) {
				event.result = handle.apply( cur, data );
				if ( event.result === false ) {
					event.preventDefault();
				}
			}
		}
		event.type = type;

		// If nobody prevented the default action, do it now
		if ( !onlyHandlers && !event.isDefaultPrevented() ) {

			if ( (!special._default || special._default.apply( eventPath.pop(), data ) === false) &&
				jQuery.acceptData( elem ) ) {

				// Call a native DOM method on the target with the same name name as the event.
				// Don't do default actions on window, that's where global variables be (#6170)
				if ( ontype && jQuery.isFunction( elem[ type ] ) && !jQuery.isWindow( elem ) ) {

					// Don't re-trigger an onFOO event when we call its FOO() method
					tmp = elem[ ontype ];

					if ( tmp ) {
						elem[ ontype ] = null;
					}

					// Prevent re-triggering of the same event, since we already bubbled it above
					jQuery.event.triggered = type;
					elem[ type ]();
					jQuery.event.triggered = undefined;

					if ( tmp ) {
						elem[ ontype ] = tmp;
					}
				}
			}
		}

		return event.result;
	},

	dispatch: function( event ) {

		// Make a writable jQuery.Event from the native event object
		event = jQuery.event.fix( event );

		var i, j, ret, matched, handleObj,
			handlerQueue = [],
			args = slice.call( arguments ),
			handlers = ( data_priv.get( this, "events" ) || {} )[ event.type ] || [],
			special = jQuery.event.special[ event.type ] || {};

		// Use the fix-ed jQuery.Event rather than the (read-only) native event
		args[0] = event;
		event.delegateTarget = this;

		// Call the preDispatch hook for the mapped type, and let it bail if desired
		if ( special.preDispatch && special.preDispatch.call( this, event ) === false ) {
			return;
		}

		// Determine handlers
		handlerQueue = jQuery.event.handlers.call( this, event, handlers );

		// Run delegates first; they may want to stop propagation beneath us
		i = 0;
		while ( (matched = handlerQueue[ i++ ]) && !event.isPropagationStopped() ) {
			event.currentTarget = matched.elem;

			j = 0;
			while ( (handleObj = matched.handlers[ j++ ]) && !event.isImmediatePropagationStopped() ) {

				// Triggered event must either 1) have no namespace, or
				// 2) have namespace(s) a subset or equal to those in the bound event (both can have no namespace).
				if ( !event.namespace_re || event.namespace_re.test( handleObj.namespace ) ) {

					event.handleObj = handleObj;
					event.data = handleObj.data;

					ret = ( (jQuery.event.special[ handleObj.origType ] || {}).handle || handleObj.handler )
							.apply( matched.elem, args );

					if ( ret !== undefined ) {
						if ( (event.result = ret) === false ) {
							event.preventDefault();
							event.stopPropagation();
						}
					}
				}
			}
		}

		// Call the postDispatch hook for the mapped type
		if ( special.postDispatch ) {
			special.postDispatch.call( this, event );
		}

		return event.result;
	},

	handlers: function( event, handlers ) {
		var i, matches, sel, handleObj,
			handlerQueue = [],
			delegateCount = handlers.delegateCount,
			cur = event.target;

		// Find delegate handlers
		// Black-hole SVG <use> instance trees (#13180)
		// Avoid non-left-click bubbling in Firefox (#3861)
		if ( delegateCount && cur.nodeType && (!event.button || event.type !== "click") ) {

			for ( ; cur !== this; cur = cur.parentNode || this ) {

				// Don't process clicks on disabled elements (#6911, #8165, #11382, #11764)
				if ( cur.disabled !== true || event.type !== "click" ) {
					matches = [];
					for ( i = 0; i < delegateCount; i++ ) {
						handleObj = handlers[ i ];

						// Don't conflict with Object.prototype properties (#13203)
						sel = handleObj.selector + " ";

						if ( matches[ sel ] === undefined ) {
							matches[ sel ] = handleObj.needsContext ?
								jQuery( sel, this ).index( cur ) >= 0 :
								jQuery.find( sel, this, null, [ cur ] ).length;
						}
						if ( matches[ sel ] ) {
							matches.push( handleObj );
						}
					}
					if ( matches.length ) {
						handlerQueue.push({ elem: cur, handlers: matches });
					}
				}
			}
		}

		// Add the remaining (directly-bound) handlers
		if ( delegateCount < handlers.length ) {
			handlerQueue.push({ elem: this, handlers: handlers.slice( delegateCount ) });
		}

		return handlerQueue;
	},

	// Includes some event props shared by KeyEvent and MouseEvent
	props: "altKey bubbles cancelable ctrlKey currentTarget eventPhase metaKey relatedTarget shiftKey target timeStamp view which".split(" "),

	fixHooks: {},

	keyHooks: {
		props: "char charCode key keyCode".split(" "),
		filter: function( event, original ) {

			// Add which for key events
			if ( event.which == null ) {
				event.which = original.charCode != null ? original.charCode : original.keyCode;
			}

			return event;
		}
	},

	mouseHooks: {
		props: "button buttons clientX clientY offsetX offsetY pageX pageY screenX screenY toElement".split(" "),
		filter: function( event, original ) {
			var eventDoc, doc, body,
				button = original.button;

			// Calculate pageX/Y if missing and clientX/Y available
			if ( event.pageX == null && original.clientX != null ) {
				eventDoc = event.target.ownerDocument || document;
				doc = eventDoc.documentElement;
				body = eventDoc.body;

				event.pageX = original.clientX + ( doc && doc.scrollLeft || body && body.scrollLeft || 0 ) - ( doc && doc.clientLeft || body && body.clientLeft || 0 );
				event.pageY = original.clientY + ( doc && doc.scrollTop  || body && body.scrollTop  || 0 ) - ( doc && doc.clientTop  || body && body.clientTop  || 0 );
			}

			// Add which for click: 1 === left; 2 === middle; 3 === right
			// Note: button is not normalized, so don't use it
			if ( !event.which && button !== undefined ) {
				event.which = ( button & 1 ? 1 : ( button & 2 ? 3 : ( button & 4 ? 2 : 0 ) ) );
			}

			return event;
		}
	},

	fix: function( event ) {
		if ( event[ jQuery.expando ] ) {
			return event;
		}

		// Create a writable copy of the event object and normalize some properties
		var i, prop, copy,
			type = event.type,
			originalEvent = event,
			fixHook = this.fixHooks[ type ];

		if ( !fixHook ) {
			this.fixHooks[ type ] = fixHook =
				rmouseEvent.test( type ) ? this.mouseHooks :
				rkeyEvent.test( type ) ? this.keyHooks :
				{};
		}
		copy = fixHook.props ? this.props.concat( fixHook.props ) : this.props;

		event = new jQuery.Event( originalEvent );

		i = copy.length;
		while ( i-- ) {
			prop = copy[ i ];
			event[ prop ] = originalEvent[ prop ];
		}

		// Support: Cordova 2.5 (WebKit) (#13255)
		// All events should have a target; Cordova deviceready doesn't
		if ( !event.target ) {
			event.target = document;
		}

		// Support: Safari 6.0+, Chrome < 28
		// Target should not be a text node (#504, #13143)
		if ( event.target.nodeType === 3 ) {
			event.target = event.target.parentNode;
		}

		return fixHook.filter ? fixHook.filter( event, originalEvent ) : event;
	},

	special: {
		load: {
			// Prevent triggered image.load events from bubbling to window.load
			noBubble: true
		},
		focus: {
			// Fire native event if possible so blur/focus sequence is correct
			trigger: function() {
				if ( this !== safeActiveElement() && this.focus ) {
					this.focus();
					return false;
				}
			},
			delegateType: "focusin"
		},
		blur: {
			trigger: function() {
				if ( this === safeActiveElement() && this.blur ) {
					this.blur();
					return false;
				}
			},
			delegateType: "focusout"
		},
		click: {
			// For checkbox, fire native event so checked state will be right
			trigger: function() {
				if ( this.type === "checkbox" && this.click && jQuery.nodeName( this, "input" ) ) {
					this.click();
					return false;
				}
			},

			// For cross-browser consistency, don't fire native .click() on links
			_default: function( event ) {
				return jQuery.nodeName( event.target, "a" );
			}
		},

		beforeunload: {
			postDispatch: function( event ) {

				// Support: Firefox 20+
				// Firefox doesn't alert if the returnValue field is not set.
				if ( event.result !== undefined && event.originalEvent ) {
					event.originalEvent.returnValue = event.result;
				}
			}
		}
	},

	simulate: function( type, elem, event, bubble ) {
		// Piggyback on a donor event to simulate a different one.
		// Fake originalEvent to avoid donor's stopPropagation, but if the
		// simulated event prevents default then we do the same on the donor.
		var e = jQuery.extend(
			new jQuery.Event(),
			event,
			{
				type: type,
				isSimulated: true,
				originalEvent: {}
			}
		);
		if ( bubble ) {
			jQuery.event.trigger( e, null, elem );
		} else {
			jQuery.event.dispatch.call( elem, e );
		}
		if ( e.isDefaultPrevented() ) {
			event.preventDefault();
		}
	}
};

jQuery.removeEvent = function( elem, type, handle ) {
	if ( elem.removeEventListener ) {
		elem.removeEventListener( type, handle, false );
	}
};

jQuery.Event = function( src, props ) {
	// Allow instantiation without the 'new' keyword
	if ( !(this instanceof jQuery.Event) ) {
		return new jQuery.Event( src, props );
	}

	// Event object
	if ( src && src.type ) {
		this.originalEvent = src;
		this.type = src.type;

		// Events bubbling up the document may have been marked as prevented
		// by a handler lower down the tree; reflect the correct value.
		this.isDefaultPrevented = src.defaultPrevented ||
				src.defaultPrevented === undefined &&
				// Support: Android < 4.0
				src.returnValue === false ?
			returnTrue :
			returnFalse;

	// Event type
	} else {
		this.type = src;
	}

	// Put explicitly provided properties onto the event object
	if ( props ) {
		jQuery.extend( this, props );
	}

	// Create a timestamp if incoming event doesn't have one
	this.timeStamp = src && src.timeStamp || jQuery.now();

	// Mark it as fixed
	this[ jQuery.expando ] = true;
};

// jQuery.Event is based on DOM3 Events as specified by the ECMAScript Language Binding
// http://www.w3.org/TR/2003/WD-DOM-Level-3-Events-20030331/ecma-script-binding.html
jQuery.Event.prototype = {
	isDefaultPrevented: returnFalse,
	isPropagationStopped: returnFalse,
	isImmediatePropagationStopped: returnFalse,

	preventDefault: function() {
		var e = this.originalEvent;

		this.isDefaultPrevented = returnTrue;

		if ( e && e.preventDefault ) {
			e.preventDefault();
		}
	},
	stopPropagation: function() {
		var e = this.originalEvent;

		this.isPropagationStopped = returnTrue;

		if ( e && e.stopPropagation ) {
			e.stopPropagation();
		}
	},
	stopImmediatePropagation: function() {
		var e = this.originalEvent;

		this.isImmediatePropagationStopped = returnTrue;

		if ( e && e.stopImmediatePropagation ) {
			e.stopImmediatePropagation();
		}

		this.stopPropagation();
	}
};

// Create mouseenter/leave events using mouseover/out and event-time checks
// Support: Chrome 15+
jQuery.each({
	mouseenter: "mouseover",
	mouseleave: "mouseout",
	pointerenter: "pointerover",
	pointerleave: "pointerout"
}, function( orig, fix ) {
	jQuery.event.special[ orig ] = {
		delegateType: fix,
		bindType: fix,

		handle: function( event ) {
			var ret,
				target = this,
				related = event.relatedTarget,
				handleObj = event.handleObj;

			// For mousenter/leave call the handler if related is outside the target.
			// NB: No relatedTarget if the mouse left/entered the browser window
			if ( !related || (related !== target && !jQuery.contains( target, related )) ) {
				event.type = handleObj.origType;
				ret = handleObj.handler.apply( this, arguments );
				event.type = fix;
			}
			return ret;
		}
	};
});

// Create "bubbling" focus and blur events
// Support: Firefox, Chrome, Safari
if ( !support.focusinBubbles ) {
	jQuery.each({ focus: "focusin", blur: "focusout" }, function( orig, fix ) {

		// Attach a single capturing handler on the document while someone wants focusin/focusout
		var handler = function( event ) {
				jQuery.event.simulate( fix, event.target, jQuery.event.fix( event ), true );
			};

		jQuery.event.special[ fix ] = {
			setup: function() {
				var doc = this.ownerDocument || this,
					attaches = data_priv.access( doc, fix );

				if ( !attaches ) {
					doc.addEventListener( orig, handler, true );
				}
				data_priv.access( doc, fix, ( attaches || 0 ) + 1 );
			},
			teardown: function() {
				var doc = this.ownerDocument || this,
					attaches = data_priv.access( doc, fix ) - 1;

				if ( !attaches ) {
					doc.removeEventListener( orig, handler, true );
					data_priv.remove( doc, fix );

				} else {
					data_priv.access( doc, fix, attaches );
				}
			}
		};
	});
}

jQuery.fn.extend({

	on: function( types, selector, data, fn, /*INTERNAL*/ one ) {
		var origFn, type;

		// Types can be a map of types/handlers
		if ( typeof types === "object" ) {
			// ( types-Object, selector, data )
			if ( typeof selector !== "string" ) {
				// ( types-Object, data )
				data = data || selector;
				selector = undefined;
			}
			for ( type in types ) {
				this.on( type, selector, data, types[ type ], one );
			}
			return this;
		}

		if ( data == null && fn == null ) {
			// ( types, fn )
			fn = selector;
			data = selector = undefined;
		} else if ( fn == null ) {
			if ( typeof selector === "string" ) {
				// ( types, selector, fn )
				fn = data;
				data = undefined;
			} else {
				// ( types, data, fn )
				fn = data;
				data = selector;
				selector = undefined;
			}
		}
		if ( fn === false ) {
			fn = returnFalse;
		} else if ( !fn ) {
			return this;
		}

		if ( one === 1 ) {
			origFn = fn;
			fn = function( event ) {
				// Can use an empty set, since event contains the info
				jQuery().off( event );
				return origFn.apply( this, arguments );
			};
			// Use same guid so caller can remove using origFn
			fn.guid = origFn.guid || ( origFn.guid = jQuery.guid++ );
		}
		return this.each( function() {
			jQuery.event.add( this, types, fn, data, selector );
		});
	},
	one: function( types, selector, data, fn ) {
		return this.on( types, selector, data, fn, 1 );
	},
	off: function( types, selector, fn ) {
		var handleObj, type;
		if ( types && types.preventDefault && types.handleObj ) {
			// ( event )  dispatched jQuery.Event
			handleObj = types.handleObj;
			jQuery( types.delegateTarget ).off(
				handleObj.namespace ? handleObj.origType + "." + handleObj.namespace : handleObj.origType,
				handleObj.selector,
				handleObj.handler
			);
			return this;
		}
		if ( typeof types === "object" ) {
			// ( types-object [, selector] )
			for ( type in types ) {
				this.off( type, selector, types[ type ] );
			}
			return this;
		}
		if ( selector === false || typeof selector === "function" ) {
			// ( types [, fn] )
			fn = selector;
			selector = undefined;
		}
		if ( fn === false ) {
			fn = returnFalse;
		}
		return this.each(function() {
			jQuery.event.remove( this, types, fn, selector );
		});
	},

	trigger: function( type, data ) {
		return this.each(function() {
			jQuery.event.trigger( type, data, this );
		});
	},
	triggerHandler: function( type, data ) {
		var elem = this[0];
		if ( elem ) {
			return jQuery.event.trigger( type, data, elem, true );
		}
	}
});


var
	rxhtmlTag = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/gi,
	rtagName = /<([\w:]+)/,
	rhtml = /<|&#?\w+;/,
	rnoInnerhtml = /<(?:script|style|link)/i,
	// checked="checked" or checked
	rchecked = /checked\s*(?:[^=]|=\s*.checked.)/i,
	rscriptType = /^$|\/(?:java|ecma)script/i,
	rscriptTypeMasked = /^true\/(.*)/,
	rcleanScript = /^\s*<!(?:\[CDATA\[|--)|(?:\]\]|--)>\s*$/g,

	// We have to close these tags to support XHTML (#13200)
	wrapMap = {

		// Support: IE 9
		option: [ 1, "<select multiple='multiple'>", "</select>" ],

		thead: [ 1, "<table>", "</table>" ],
		col: [ 2, "<table><colgroup>", "</colgroup></table>" ],
		tr: [ 2, "<table><tbody>", "</tbody></table>" ],
		td: [ 3, "<table><tbody><tr>", "</tr></tbody></table>" ],

		_default: [ 0, "", "" ]
	};

// Support: IE 9
wrapMap.optgroup = wrapMap.option;

wrapMap.tbody = wrapMap.tfoot = wrapMap.colgroup = wrapMap.caption = wrapMap.thead;
wrapMap.th = wrapMap.td;

// Support: 1.x compatibility
// Manipulating tables requires a tbody
function manipulationTarget( elem, content ) {
	return jQuery.nodeName( elem, "table" ) &&
		jQuery.nodeName( content.nodeType !== 11 ? content : content.firstChild, "tr" ) ?

		elem.getElementsByTagName("tbody")[0] ||
			elem.appendChild( elem.ownerDocument.createElement("tbody") ) :
		elem;
}

// Replace/restore the type attribute of script elements for safe DOM manipulation
function disableScript( elem ) {
	elem.type = (elem.getAttribute("type") !== null) + "/" + elem.type;
	return elem;
}
function restoreScript( elem ) {
	var match = rscriptTypeMasked.exec( elem.type );

	if ( match ) {
		elem.type = match[ 1 ];
	} else {
		elem.removeAttribute("type");
	}

	return elem;
}

// Mark scripts as having already been evaluated
function setGlobalEval( elems, refElements ) {
	var i = 0,
		l = elems.length;

	for ( ; i < l; i++ ) {
		data_priv.set(
			elems[ i ], "globalEval", !refElements || data_priv.get( refElements[ i ], "globalEval" )
		);
	}
}

function cloneCopyEvent( src, dest ) {
	var i, l, type, pdataOld, pdataCur, udataOld, udataCur, events;

	if ( dest.nodeType !== 1 ) {
		return;
	}

	// 1. Copy private data: events, handlers, etc.
	if ( data_priv.hasData( src ) ) {
		pdataOld = data_priv.access( src );
		pdataCur = data_priv.set( dest, pdataOld );
		events = pdataOld.events;

		if ( events ) {
			delete pdataCur.handle;
			pdataCur.events = {};

			for ( type in events ) {
				for ( i = 0, l = events[ type ].length; i < l; i++ ) {
					jQuery.event.add( dest, type, events[ type ][ i ] );
				}
			}
		}
	}

	// 2. Copy user data
	if ( data_user.hasData( src ) ) {
		udataOld = data_user.access( src );
		udataCur = jQuery.extend( {}, udataOld );

		data_user.set( dest, udataCur );
	}
}

function getAll( context, tag ) {
	var ret = context.getElementsByTagName ? context.getElementsByTagName( tag || "*" ) :
			context.querySelectorAll ? context.querySelectorAll( tag || "*" ) :
			[];

	return tag === undefined || tag && jQuery.nodeName( context, tag ) ?
		jQuery.merge( [ context ], ret ) :
		ret;
}

// Support: IE >= 9
function fixInput( src, dest ) {
	var nodeName = dest.nodeName.toLowerCase();

	// Fails to persist the checked state of a cloned checkbox or radio button.
	if ( nodeName === "input" && rcheckableType.test( src.type ) ) {
		dest.checked = src.checked;

	// Fails to return the selected option to the default selected state when cloning options
	} else if ( nodeName === "input" || nodeName === "textarea" ) {
		dest.defaultValue = src.defaultValue;
	}
}

jQuery.extend({
	clone: function( elem, dataAndEvents, deepDataAndEvents ) {
		var i, l, srcElements, destElements,
			clone = elem.cloneNode( true ),
			inPage = jQuery.contains( elem.ownerDocument, elem );

		// Support: IE >= 9
		// Fix Cloning issues
		if ( !support.noCloneChecked && ( elem.nodeType === 1 || elem.nodeType === 11 ) &&
				!jQuery.isXMLDoc( elem ) ) {

			// We eschew Sizzle here for performance reasons: http://jsperf.com/getall-vs-sizzle/2
			destElements = getAll( clone );
			srcElements = getAll( elem );

			for ( i = 0, l = srcElements.length; i < l; i++ ) {
				fixInput( srcElements[ i ], destElements[ i ] );
			}
		}

		// Copy the events from the original to the clone
		if ( dataAndEvents ) {
			if ( deepDataAndEvents ) {
				srcElements = srcElements || getAll( elem );
				destElements = destElements || getAll( clone );

				for ( i = 0, l = srcElements.length; i < l; i++ ) {
					cloneCopyEvent( srcElements[ i ], destElements[ i ] );
				}
			} else {
				cloneCopyEvent( elem, clone );
			}
		}

		// Preserve script evaluation history
		destElements = getAll( clone, "script" );
		if ( destElements.length > 0 ) {
			setGlobalEval( destElements, !inPage && getAll( elem, "script" ) );
		}

		// Return the cloned set
		return clone;
	},

	buildFragment: function( elems, context, scripts, selection ) {
		var elem, tmp, tag, wrap, contains, j,
			fragment = context.createDocumentFragment(),
			nodes = [],
			i = 0,
			l = elems.length;

		for ( ; i < l; i++ ) {
			elem = elems[ i ];

			if ( elem || elem === 0 ) {

				// Add nodes directly
				if ( jQuery.type( elem ) === "object" ) {
					// Support: QtWebKit
					// jQuery.merge because push.apply(_, arraylike) throws
					jQuery.merge( nodes, elem.nodeType ? [ elem ] : elem );

				// Convert non-html into a text node
				} else if ( !rhtml.test( elem ) ) {
					nodes.push( context.createTextNode( elem ) );

				// Convert html into DOM nodes
				} else {
					tmp = tmp || fragment.appendChild( context.createElement("div") );

					// Deserialize a standard representation
					tag = ( rtagName.exec( elem ) || [ "", "" ] )[ 1 ].toLowerCase();
					wrap = wrapMap[ tag ] || wrapMap._default;
					tmp.innerHTML = wrap[ 1 ] + elem.replace( rxhtmlTag, "<$1></$2>" ) + wrap[ 2 ];

					// Descend through wrappers to the right content
					j = wrap[ 0 ];
					while ( j-- ) {
						tmp = tmp.lastChild;
					}

					// Support: QtWebKit
					// jQuery.merge because push.apply(_, arraylike) throws
					jQuery.merge( nodes, tmp.childNodes );

					// Remember the top-level container
					tmp = fragment.firstChild;

					// Fixes #12346
					// Support: Webkit, IE
					tmp.textContent = "";
				}
			}
		}

		// Remove wrapper from fragment
		fragment.textContent = "";

		i = 0;
		while ( (elem = nodes[ i++ ]) ) {

			// #4087 - If origin and destination elements are the same, and this is
			// that element, do not do anything
			if ( selection && jQuery.inArray( elem, selection ) !== -1 ) {
				continue;
			}

			contains = jQuery.contains( elem.ownerDocument, elem );

			// Append to fragment
			tmp = getAll( fragment.appendChild( elem ), "script" );

			// Preserve script evaluation history
			if ( contains ) {
				setGlobalEval( tmp );
			}

			// Capture executables
			if ( scripts ) {
				j = 0;
				while ( (elem = tmp[ j++ ]) ) {
					if ( rscriptType.test( elem.type || "" ) ) {
						scripts.push( elem );
					}
				}
			}
		}

		return fragment;
	},

	cleanData: function( elems ) {
		var data, elem, type, key,
			special = jQuery.event.special,
			i = 0;

		for ( ; (elem = elems[ i ]) !== undefined; i++ ) {
			if ( jQuery.acceptData( elem ) ) {
				key = elem[ data_priv.expando ];

				if ( key && (data = data_priv.cache[ key ]) ) {
					if ( data.events ) {
						for ( type in data.events ) {
							if ( special[ type ] ) {
								jQuery.event.remove( elem, type );

							// This is a shortcut to avoid jQuery.event.remove's overhead
							} else {
								jQuery.removeEvent( elem, type, data.handle );
							}
						}
					}
					if ( data_priv.cache[ key ] ) {
						// Discard any remaining `private` data
						delete data_priv.cache[ key ];
					}
				}
			}
			// Discard any remaining `user` data
			delete data_user.cache[ elem[ data_user.expando ] ];
		}
	}
});

jQuery.fn.extend({
	text: function( value ) {
		return access( this, function( value ) {
			return value === undefined ?
				jQuery.text( this ) :
				this.empty().each(function() {
					if ( this.nodeType === 1 || this.nodeType === 11 || this.nodeType === 9 ) {
						this.textContent = value;
					}
				});
		}, null, value, arguments.length );
	},

	append: function() {
		return this.domManip( arguments, function( elem ) {
			if ( this.nodeType === 1 || this.nodeType === 11 || this.nodeType === 9 ) {
				var target = manipulationTarget( this, elem );
				target.appendChild( elem );
			}
		});
	},

	prepend: function() {
		return this.domManip( arguments, function( elem ) {
			if ( this.nodeType === 1 || this.nodeType === 11 || this.nodeType === 9 ) {
				var target = manipulationTarget( this, elem );
				target.insertBefore( elem, target.firstChild );
			}
		});
	},

	before: function() {
		return this.domManip( arguments, function( elem ) {
			if ( this.parentNode ) {
				this.parentNode.insertBefore( elem, this );
			}
		});
	},

	after: function() {
		return this.domManip( arguments, function( elem ) {
			if ( this.parentNode ) {
				this.parentNode.insertBefore( elem, this.nextSibling );
			}
		});
	},

	remove: function( selector, keepData /* Internal Use Only */ ) {
		var elem,
			elems = selector ? jQuery.filter( selector, this ) : this,
			i = 0;

		for ( ; (elem = elems[i]) != null; i++ ) {
			if ( !keepData && elem.nodeType === 1 ) {
				jQuery.cleanData( getAll( elem ) );
			}

			if ( elem.parentNode ) {
				if ( keepData && jQuery.contains( elem.ownerDocument, elem ) ) {
					setGlobalEval( getAll( elem, "script" ) );
				}
				elem.parentNode.removeChild( elem );
			}
		}

		return this;
	},

	empty: function() {
		var elem,
			i = 0;

		for ( ; (elem = this[i]) != null; i++ ) {
			if ( elem.nodeType === 1 ) {

				// Prevent memory leaks
				jQuery.cleanData( getAll( elem, false ) );

				// Remove any remaining nodes
				elem.textContent = "";
			}
		}

		return this;
	},

	clone: function( dataAndEvents, deepDataAndEvents ) {
		dataAndEvents = dataAndEvents == null ? false : dataAndEvents;
		deepDataAndEvents = deepDataAndEvents == null ? dataAndEvents : deepDataAndEvents;

		return this.map(function() {
			return jQuery.clone( this, dataAndEvents, deepDataAndEvents );
		});
	},

	html: function( value ) {
		return access( this, function( value ) {
			var elem = this[ 0 ] || {},
				i = 0,
				l = this.length;

			if ( value === undefined && elem.nodeType === 1 ) {
				return elem.innerHTML;
			}

			// See if we can take a shortcut and just use innerHTML
			if ( typeof value === "string" && !rnoInnerhtml.test( value ) &&
				!wrapMap[ ( rtagName.exec( value ) || [ "", "" ] )[ 1 ].toLowerCase() ] ) {

				value = value.replace( rxhtmlTag, "<$1></$2>" );

				try {
					for ( ; i < l; i++ ) {
						elem = this[ i ] || {};

						// Remove element nodes and prevent memory leaks
						if ( elem.nodeType === 1 ) {
							jQuery.cleanData( getAll( elem, false ) );
							elem.innerHTML = value;
						}
					}

					elem = 0;

				// If using innerHTML throws an exception, use the fallback method
				} catch( e ) {}
			}

			if ( elem ) {
				this.empty().append( value );
			}
		}, null, value, arguments.length );
	},

	replaceWith: function() {
		var arg = arguments[ 0 ];

		// Make the changes, replacing each context element with the new content
		this.domManip( arguments, function( elem ) {
			arg = this.parentNode;

			jQuery.cleanData( getAll( this ) );

			if ( arg ) {
				arg.replaceChild( elem, this );
			}
		});

		// Force removal if there was no new content (e.g., from empty arguments)
		return arg && (arg.length || arg.nodeType) ? this : this.remove();
	},

	detach: function( selector ) {
		return this.remove( selector, true );
	},

	domManip: function( args, callback ) {

		// Flatten any nested arrays
		args = concat.apply( [], args );

		var fragment, first, scripts, hasScripts, node, doc,
			i = 0,
			l = this.length,
			set = this,
			iNoClone = l - 1,
			value = args[ 0 ],
			isFunction = jQuery.isFunction( value );

		// We can't cloneNode fragments that contain checked, in WebKit
		if ( isFunction ||
				( l > 1 && typeof value === "string" &&
					!support.checkClone && rchecked.test( value ) ) ) {
			return this.each(function( index ) {
				var self = set.eq( index );
				if ( isFunction ) {
					args[ 0 ] = value.call( this, index, self.html() );
				}
				self.domManip( args, callback );
			});
		}

		if ( l ) {
			fragment = jQuery.buildFragment( args, this[ 0 ].ownerDocument, false, this );
			first = fragment.firstChild;

			if ( fragment.childNodes.length === 1 ) {
				fragment = first;
			}

			if ( first ) {
				scripts = jQuery.map( getAll( fragment, "script" ), disableScript );
				hasScripts = scripts.length;

				// Use the original fragment for the last item instead of the first because it can end up
				// being emptied incorrectly in certain situations (#8070).
				for ( ; i < l; i++ ) {
					node = fragment;

					if ( i !== iNoClone ) {
						node = jQuery.clone( node, true, true );

						// Keep references to cloned scripts for later restoration
						if ( hasScripts ) {
							// Support: QtWebKit
							// jQuery.merge because push.apply(_, arraylike) throws
							jQuery.merge( scripts, getAll( node, "script" ) );
						}
					}

					callback.call( this[ i ], node, i );
				}

				if ( hasScripts ) {
					doc = scripts[ scripts.length - 1 ].ownerDocument;

					// Reenable scripts
					jQuery.map( scripts, restoreScript );

					// Evaluate executable scripts on first document insertion
					for ( i = 0; i < hasScripts; i++ ) {
						node = scripts[ i ];
						if ( rscriptType.test( node.type || "" ) &&
							!data_priv.access( node, "globalEval" ) && jQuery.contains( doc, node ) ) {

							if ( node.src ) {
								// Optional AJAX dependency, but won't run scripts if not present
								if ( jQuery._evalUrl ) {
									jQuery._evalUrl( node.src );
								}
							} else {
								jQuery.globalEval( node.textContent.replace( rcleanScript, "" ) );
							}
						}
					}
				}
			}
		}

		return this;
	}
});

jQuery.each({
	appendTo: "append",
	prependTo: "prepend",
	insertBefore: "before",
	insertAfter: "after",
	replaceAll: "replaceWith"
}, function( name, original ) {
	jQuery.fn[ name ] = function( selector ) {
		var elems,
			ret = [],
			insert = jQuery( selector ),
			last = insert.length - 1,
			i = 0;

		for ( ; i <= last; i++ ) {
			elems = i === last ? this : this.clone( true );
			jQuery( insert[ i ] )[ original ]( elems );

			// Support: QtWebKit
			// .get() because push.apply(_, arraylike) throws
			push.apply( ret, elems.get() );
		}

		return this.pushStack( ret );
	};
});


var iframe,
	elemdisplay = {};

/**
 * Retrieve the actual display of a element
 * @param {String} name nodeName of the element
 * @param {Object} doc Document object
 */
// Called only from within defaultDisplay
function actualDisplay( name, doc ) {
	var style,
		elem = jQuery( doc.createElement( name ) ).appendTo( doc.body ),

		// getDefaultComputedStyle might be reliably used only on attached element
		display = window.getDefaultComputedStyle && ( style = window.getDefaultComputedStyle( elem[ 0 ] ) ) ?

			// Use of this method is a temporary fix (more like optmization) until something better comes along,
			// since it was removed from specification and supported only in FF
			style.display : jQuery.css( elem[ 0 ], "display" );

	// We don't have any data stored on the element,
	// so use "detach" method as fast way to get rid of the element
	elem.detach();

	return display;
}

/**
 * Try to determine the default display value of an element
 * @param {String} nodeName
 */
function defaultDisplay( nodeName ) {
	var doc = document,
		display = elemdisplay[ nodeName ];

	if ( !display ) {
		display = actualDisplay( nodeName, doc );

		// If the simple way fails, read from inside an iframe
		if ( display === "none" || !display ) {

			// Use the already-created iframe if possible
			iframe = (iframe || jQuery( "<iframe frameborder='0' width='0' height='0'/>" )).appendTo( doc.documentElement );

			// Always write a new HTML skeleton so Webkit and Firefox don't choke on reuse
			doc = iframe[ 0 ].contentDocument;

			// Support: IE
			doc.write();
			doc.close();

			display = actualDisplay( nodeName, doc );
			iframe.detach();
		}

		// Store the correct default display
		elemdisplay[ nodeName ] = display;
	}

	return display;
}
var rmargin = (/^margin/);

var rnumnonpx = new RegExp( "^(" + pnum + ")(?!px)[a-z%]+$", "i" );

var getStyles = function( elem ) {
		return elem.ownerDocument.defaultView.getComputedStyle( elem, null );
	};



function curCSS( elem, name, computed ) {
	var width, minWidth, maxWidth, ret,
		style = elem.style;

	computed = computed || getStyles( elem );

	// Support: IE9
	// getPropertyValue is only needed for .css('filter') in IE9, see #12537
	if ( computed ) {
		ret = computed.getPropertyValue( name ) || computed[ name ];
	}

	if ( computed ) {

		if ( ret === "" && !jQuery.contains( elem.ownerDocument, elem ) ) {
			ret = jQuery.style( elem, name );
		}

		// Support: iOS < 6
		// A tribute to the "awesome hack by Dean Edwards"
		// iOS < 6 (at least) returns percentage for a larger set of values, but width seems to be reliably pixels
		// this is against the CSSOM draft spec: http://dev.w3.org/csswg/cssom/#resolved-values
		if ( rnumnonpx.test( ret ) && rmargin.test( name ) ) {

			// Remember the original values
			width = style.width;
			minWidth = style.minWidth;
			maxWidth = style.maxWidth;

			// Put in the new values to get a computed value out
			style.minWidth = style.maxWidth = style.width = ret;
			ret = computed.width;

			// Revert the changed values
			style.width = width;
			style.minWidth = minWidth;
			style.maxWidth = maxWidth;
		}
	}

	return ret !== undefined ?
		// Support: IE
		// IE returns zIndex value as an integer.
		ret + "" :
		ret;
}


function addGetHookIf( conditionFn, hookFn ) {
	// Define the hook, we'll check on the first run if it's really needed.
	return {
		get: function() {
			if ( conditionFn() ) {
				// Hook not needed (or it's not possible to use it due to missing dependency),
				// remove it.
				// Since there are no other hooks for marginRight, remove the whole object.
				delete this.get;
				return;
			}

			// Hook needed; redefine it so that the support test is not executed again.

			return (this.get = hookFn).apply( this, arguments );
		}
	};
}


(function() {
	var pixelPositionVal, boxSizingReliableVal,
		docElem = document.documentElement,
		container = document.createElement( "div" ),
		div = document.createElement( "div" );

	if ( !div.style ) {
		return;
	}

	div.style.backgroundClip = "content-box";
	div.cloneNode( true ).style.backgroundClip = "";
	support.clearCloneStyle = div.style.backgroundClip === "content-box";

	container.style.cssText = "border:0;width:0;height:0;top:0;left:-9999px;margin-top:1px;" +
		"position:absolute";
	container.appendChild( div );

	// Executing both pixelPosition & boxSizingReliable tests require only one layout
	// so they're executed at the same time to save the second computation.
	function computePixelPositionAndBoxSizingReliable() {
		div.style.cssText =
			// Support: Firefox<29, Android 2.3
			// Vendor-prefix box-sizing
			"-webkit-box-sizing:border-box;-moz-box-sizing:border-box;" +
			"box-sizing:border-box;display:block;margin-top:1%;top:1%;" +
			"border:1px;padding:1px;width:4px;position:absolute";
		div.innerHTML = "";
		docElem.appendChild( container );

		var divStyle = window.getComputedStyle( div, null );
		pixelPositionVal = divStyle.top !== "1%";
		boxSizingReliableVal = divStyle.width === "4px";

		docElem.removeChild( container );
	}

	// Support: node.js jsdom
	// Don't assume that getComputedStyle is a property of the global object
	if ( window.getComputedStyle ) {
		jQuery.extend( support, {
			pixelPosition: function() {
				// This test is executed only once but we still do memoizing
				// since we can use the boxSizingReliable pre-computing.
				// No need to check if the test was already performed, though.
				computePixelPositionAndBoxSizingReliable();
				return pixelPositionVal;
			},
			boxSizingReliable: function() {
				if ( boxSizingReliableVal == null ) {
					computePixelPositionAndBoxSizingReliable();
				}
				return boxSizingReliableVal;
			},
			reliableMarginRight: function() {
				// Support: Android 2.3
				// Check if div with explicit width and no margin-right incorrectly
				// gets computed margin-right based on width of container. (#3333)
				// WebKit Bug 13343 - getComputedStyle returns wrong value for margin-right
				// This support function is only executed once so no memoizing is needed.
				var ret,
					marginDiv = div.appendChild( document.createElement( "div" ) );

				// Reset CSS: box-sizing; display; margin; border; padding
				marginDiv.style.cssText = div.style.cssText =
					// Support: Firefox<29, Android 2.3
					// Vendor-prefix box-sizing
					"-webkit-box-sizing:content-box;-moz-box-sizing:content-box;" +
					"box-sizing:content-box;display:block;margin:0;border:0;padding:0";
				marginDiv.style.marginRight = marginDiv.style.width = "0";
				div.style.width = "1px";
				docElem.appendChild( container );

				ret = !parseFloat( window.getComputedStyle( marginDiv, null ).marginRight );

				docElem.removeChild( container );

				return ret;
			}
		});
	}
})();


// A method for quickly swapping in/out CSS properties to get correct calculations.
jQuery.swap = function( elem, options, callback, args ) {
	var ret, name,
		old = {};

	// Remember the old values, and insert the new ones
	for ( name in options ) {
		old[ name ] = elem.style[ name ];
		elem.style[ name ] = options[ name ];
	}

	ret = callback.apply( elem, args || [] );

	// Revert the old values
	for ( name in options ) {
		elem.style[ name ] = old[ name ];
	}

	return ret;
};


var
	// swappable if display is none or starts with table except "table", "table-cell", or "table-caption"
	// see here for display values: https://developer.mozilla.org/en-US/docs/CSS/display
	rdisplayswap = /^(none|table(?!-c[ea]).+)/,
	rnumsplit = new RegExp( "^(" + pnum + ")(.*)$", "i" ),
	rrelNum = new RegExp( "^([+-])=(" + pnum + ")", "i" ),

	cssShow = { position: "absolute", visibility: "hidden", display: "block" },
	cssNormalTransform = {
		letterSpacing: "0",
		fontWeight: "400"
	},

	cssPrefixes = [ "Webkit", "O", "Moz", "ms" ];

// return a css property mapped to a potentially vendor prefixed property
function vendorPropName( style, name ) {

	// shortcut for names that are not vendor prefixed
	if ( name in style ) {
		return name;
	}

	// check for vendor prefixed names
	var capName = name[0].toUpperCase() + name.slice(1),
		origName = name,
		i = cssPrefixes.length;

	while ( i-- ) {
		name = cssPrefixes[ i ] + capName;
		if ( name in style ) {
			return name;
		}
	}

	return origName;
}

function setPositiveNumber( elem, value, subtract ) {
	var matches = rnumsplit.exec( value );
	return matches ?
		// Guard against undefined "subtract", e.g., when used as in cssHooks
		Math.max( 0, matches[ 1 ] - ( subtract || 0 ) ) + ( matches[ 2 ] || "px" ) :
		value;
}

function augmentWidthOrHeight( elem, name, extra, isBorderBox, styles ) {
	var i = extra === ( isBorderBox ? "border" : "content" ) ?
		// If we already have the right measurement, avoid augmentation
		4 :
		// Otherwise initialize for horizontal or vertical properties
		name === "width" ? 1 : 0,

		val = 0;

	for ( ; i < 4; i += 2 ) {
		// both box models exclude margin, so add it if we want it
		if ( extra === "margin" ) {
			val += jQuery.css( elem, extra + cssExpand[ i ], true, styles );
		}

		if ( isBorderBox ) {
			// border-box includes padding, so remove it if we want content
			if ( extra === "content" ) {
				val -= jQuery.css( elem, "padding" + cssExpand[ i ], true, styles );
			}

			// at this point, extra isn't border nor margin, so remove border
			if ( extra !== "margin" ) {
				val -= jQuery.css( elem, "border" + cssExpand[ i ] + "Width", true, styles );
			}
		} else {
			// at this point, extra isn't content, so add padding
			val += jQuery.css( elem, "padding" + cssExpand[ i ], true, styles );

			// at this point, extra isn't content nor padding, so add border
			if ( extra !== "padding" ) {
				val += jQuery.css( elem, "border" + cssExpand[ i ] + "Width", true, styles );
			}
		}
	}

	return val;
}

function getWidthOrHeight( elem, name, extra ) {

	// Start with offset property, which is equivalent to the border-box value
	var valueIsBorderBox = true,
		val = name === "width" ? elem.offsetWidth : elem.offsetHeight,
		styles = getStyles( elem ),
		isBorderBox = jQuery.css( elem, "boxSizing", false, styles ) === "border-box";

	// some non-html elements return undefined for offsetWidth, so check for null/undefined
	// svg - https://bugzilla.mozilla.org/show_bug.cgi?id=649285
	// MathML - https://bugzilla.mozilla.org/show_bug.cgi?id=491668
	if ( val <= 0 || val == null ) {
		// Fall back to computed then uncomputed css if necessary
		val = curCSS( elem, name, styles );
		if ( val < 0 || val == null ) {
			val = elem.style[ name ];
		}

		// Computed unit is not pixels. Stop here and return.
		if ( rnumnonpx.test(val) ) {
			return val;
		}

		// we need the check for style in case a browser which returns unreliable values
		// for getComputedStyle silently falls back to the reliable elem.style
		valueIsBorderBox = isBorderBox &&
			( support.boxSizingReliable() || val === elem.style[ name ] );

		// Normalize "", auto, and prepare for extra
		val = parseFloat( val ) || 0;
	}

	// use the active box-sizing model to add/subtract irrelevant styles
	return ( val +
		augmentWidthOrHeight(
			elem,
			name,
			extra || ( isBorderBox ? "border" : "content" ),
			valueIsBorderBox,
			styles
		)
	) + "px";
}

function showHide( elements, show ) {
	var display, elem, hidden,
		values = [],
		index = 0,
		length = elements.length;

	for ( ; index < length; index++ ) {
		elem = elements[ index ];
		if ( !elem.style ) {
			continue;
		}

		values[ index ] = data_priv.get( elem, "olddisplay" );
		display = elem.style.display;
		if ( show ) {
			// Reset the inline display of this element to learn if it is
			// being hidden by cascaded rules or not
			if ( !values[ index ] && display === "none" ) {
				elem.style.display = "";
			}

			// Set elements which have been overridden with display: none
			// in a stylesheet to whatever the default browser style is
			// for such an element
			if ( elem.style.display === "" && isHidden( elem ) ) {
				values[ index ] = data_priv.access( elem, "olddisplay", defaultDisplay(elem.nodeName) );
			}
		} else {
			hidden = isHidden( elem );

			if ( display !== "none" || !hidden ) {
				data_priv.set( elem, "olddisplay", hidden ? display : jQuery.css( elem, "display" ) );
			}
		}
	}

	// Set the display of most of the elements in a second loop
	// to avoid the constant reflow
	for ( index = 0; index < length; index++ ) {
		elem = elements[ index ];
		if ( !elem.style ) {
			continue;
		}
		if ( !show || elem.style.display === "none" || elem.style.display === "" ) {
			elem.style.display = show ? values[ index ] || "" : "none";
		}
	}

	return elements;
}

jQuery.extend({
	// Add in style property hooks for overriding the default
	// behavior of getting and setting a style property
	cssHooks: {
		opacity: {
			get: function( elem, computed ) {
				if ( computed ) {
					// We should always get a number back from opacity
					var ret = curCSS( elem, "opacity" );
					return ret === "" ? "1" : ret;
				}
			}
		}
	},

	// Don't automatically add "px" to these possibly-unitless properties
	cssNumber: {
		"columnCount": true,
		"fillOpacity": true,
		"flexGrow": true,
		"flexShrink": true,
		"fontWeight": true,
		"lineHeight": true,
		"opacity": true,
		"order": true,
		"orphans": true,
		"widows": true,
		"zIndex": true,
		"zoom": true
	},

	// Add in properties whose names you wish to fix before
	// setting or getting the value
	cssProps: {
		// normalize float css property
		"float": "cssFloat"
	},

	// Get and set the style property on a DOM Node
	style: function( elem, name, value, extra ) {
		// Don't set styles on text and comment nodes
		if ( !elem || elem.nodeType === 3 || elem.nodeType === 8 || !elem.style ) {
			return;
		}

		// Make sure that we're working with the right name
		var ret, type, hooks,
			origName = jQuery.camelCase( name ),
			style = elem.style;

		name = jQuery.cssProps[ origName ] || ( jQuery.cssProps[ origName ] = vendorPropName( style, origName ) );

		// gets hook for the prefixed version
		// followed by the unprefixed version
		hooks = jQuery.cssHooks[ name ] || jQuery.cssHooks[ origName ];

		// Check if we're setting a value
		if ( value !== undefined ) {
			type = typeof value;

			// convert relative number strings (+= or -=) to relative numbers. #7345
			if ( type === "string" && (ret = rrelNum.exec( value )) ) {
				value = ( ret[1] + 1 ) * ret[2] + parseFloat( jQuery.css( elem, name ) );
				// Fixes bug #9237
				type = "number";
			}

			// Make sure that null and NaN values aren't set. See: #7116
			if ( value == null || value !== value ) {
				return;
			}

			// If a number was passed in, add 'px' to the (except for certain CSS properties)
			if ( type === "number" && !jQuery.cssNumber[ origName ] ) {
				value += "px";
			}

			// Fixes #8908, it can be done more correctly by specifying setters in cssHooks,
			// but it would mean to define eight (for every problematic property) identical functions
			if ( !support.clearCloneStyle && value === "" && name.indexOf( "background" ) === 0 ) {
				style[ name ] = "inherit";
			}

			// If a hook was provided, use that value, otherwise just set the specified value
			if ( !hooks || !("set" in hooks) || (value = hooks.set( elem, value, extra )) !== undefined ) {
				style[ name ] = value;
			}

		} else {
			// If a hook was provided get the non-computed value from there
			if ( hooks && "get" in hooks && (ret = hooks.get( elem, false, extra )) !== undefined ) {
				return ret;
			}

			// Otherwise just get the value from the style object
			return style[ name ];
		}
	},

	css: function( elem, name, extra, styles ) {
		var val, num, hooks,
			origName = jQuery.camelCase( name );

		// Make sure that we're working with the right name
		name = jQuery.cssProps[ origName ] || ( jQuery.cssProps[ origName ] = vendorPropName( elem.style, origName ) );

		// gets hook for the prefixed version
		// followed by the unprefixed version
		hooks = jQuery.cssHooks[ name ] || jQuery.cssHooks[ origName ];

		// If a hook was provided get the computed value from there
		if ( hooks && "get" in hooks ) {
			val = hooks.get( elem, true, extra );
		}

		// Otherwise, if a way to get the computed value exists, use that
		if ( val === undefined ) {
			val = curCSS( elem, name, styles );
		}

		//convert "normal" to computed value
		if ( val === "normal" && name in cssNormalTransform ) {
			val = cssNormalTransform[ name ];
		}

		// Return, converting to number if forced or a qualifier was provided and val looks numeric
		if ( extra === "" || extra ) {
			num = parseFloat( val );
			return extra === true || jQuery.isNumeric( num ) ? num || 0 : val;
		}
		return val;
	}
});

jQuery.each([ "height", "width" ], function( i, name ) {
	jQuery.cssHooks[ name ] = {
		get: function( elem, computed, extra ) {
			if ( computed ) {
				// certain elements can have dimension info if we invisibly show them
				// however, it must have a current display style that would benefit from this
				return rdisplayswap.test( jQuery.css( elem, "display" ) ) && elem.offsetWidth === 0 ?
					jQuery.swap( elem, cssShow, function() {
						return getWidthOrHeight( elem, name, extra );
					}) :
					getWidthOrHeight( elem, name, extra );
			}
		},

		set: function( elem, value, extra ) {
			var styles = extra && getStyles( elem );
			return setPositiveNumber( elem, value, extra ?
				augmentWidthOrHeight(
					elem,
					name,
					extra,
					jQuery.css( elem, "boxSizing", false, styles ) === "border-box",
					styles
				) : 0
			);
		}
	};
});

// Support: Android 2.3
jQuery.cssHooks.marginRight = addGetHookIf( support.reliableMarginRight,
	function( elem, computed ) {
		if ( computed ) {
			// WebKit Bug 13343 - getComputedStyle returns wrong value for margin-right
			// Work around by temporarily setting element display to inline-block
			return jQuery.swap( elem, { "display": "inline-block" },
				curCSS, [ elem, "marginRight" ] );
		}
	}
);

// These hooks are used by animate to expand properties
jQuery.each({
	margin: "",
	padding: "",
	border: "Width"
}, function( prefix, suffix ) {
	jQuery.cssHooks[ prefix + suffix ] = {
		expand: function( value ) {
			var i = 0,
				expanded = {},

				// assumes a single number if not a string
				parts = typeof value === "string" ? value.split(" ") : [ value ];

			for ( ; i < 4; i++ ) {
				expanded[ prefix + cssExpand[ i ] + suffix ] =
					parts[ i ] || parts[ i - 2 ] || parts[ 0 ];
			}

			return expanded;
		}
	};

	if ( !rmargin.test( prefix ) ) {
		jQuery.cssHooks[ prefix + suffix ].set = setPositiveNumber;
	}
});

jQuery.fn.extend({
	css: function( name, value ) {
		return access( this, function( elem, name, value ) {
			var styles, len,
				map = {},
				i = 0;

			if ( jQuery.isArray( name ) ) {
				styles = getStyles( elem );
				len = name.length;

				for ( ; i < len; i++ ) {
					map[ name[ i ] ] = jQuery.css( elem, name[ i ], false, styles );
				}

				return map;
			}

			return value !== undefined ?
				jQuery.style( elem, name, value ) :
				jQuery.css( elem, name );
		}, name, value, arguments.length > 1 );
	},
	show: function() {
		return showHide( this, true );
	},
	hide: function() {
		return showHide( this );
	},
	toggle: function( state ) {
		if ( typeof state === "boolean" ) {
			return state ? this.show() : this.hide();
		}

		return this.each(function() {
			if ( isHidden( this ) ) {
				jQuery( this ).show();
			} else {
				jQuery( this ).hide();
			}
		});
	}
});


function Tween( elem, options, prop, end, easing ) {
	return new Tween.prototype.init( elem, options, prop, end, easing );
}
jQuery.Tween = Tween;

Tween.prototype = {
	constructor: Tween,
	init: function( elem, options, prop, end, easing, unit ) {
		this.elem = elem;
		this.prop = prop;
		this.easing = easing || "swing";
		this.options = options;
		this.start = this.now = this.cur();
		this.end = end;
		this.unit = unit || ( jQuery.cssNumber[ prop ] ? "" : "px" );
	},
	cur: function() {
		var hooks = Tween.propHooks[ this.prop ];

		return hooks && hooks.get ?
			hooks.get( this ) :
			Tween.propHooks._default.get( this );
	},
	run: function( percent ) {
		var eased,
			hooks = Tween.propHooks[ this.prop ];

		if ( this.options.duration ) {
			this.pos = eased = jQuery.easing[ this.easing ](
				percent, this.options.duration * percent, 0, 1, this.options.duration
			);
		} else {
			this.pos = eased = percent;
		}
		this.now = ( this.end - this.start ) * eased + this.start;

		if ( this.options.step ) {
			this.options.step.call( this.elem, this.now, this );
		}

		if ( hooks && hooks.set ) {
			hooks.set( this );
		} else {
			Tween.propHooks._default.set( this );
		}
		return this;
	}
};

Tween.prototype.init.prototype = Tween.prototype;

Tween.propHooks = {
	_default: {
		get: function( tween ) {
			var result;

			if ( tween.elem[ tween.prop ] != null &&
				(!tween.elem.style || tween.elem.style[ tween.prop ] == null) ) {
				return tween.elem[ tween.prop ];
			}

			// passing an empty string as a 3rd parameter to .css will automatically
			// attempt a parseFloat and fallback to a string if the parse fails
			// so, simple values such as "10px" are parsed to Float.
			// complex values such as "rotate(1rad)" are returned as is.
			result = jQuery.css( tween.elem, tween.prop, "" );
			// Empty strings, null, undefined and "auto" are converted to 0.
			return !result || result === "auto" ? 0 : result;
		},
		set: function( tween ) {
			// use step hook for back compat - use cssHook if its there - use .style if its
			// available and use plain properties where available
			if ( jQuery.fx.step[ tween.prop ] ) {
				jQuery.fx.step[ tween.prop ]( tween );
			} else if ( tween.elem.style && ( tween.elem.style[ jQuery.cssProps[ tween.prop ] ] != null || jQuery.cssHooks[ tween.prop ] ) ) {
				jQuery.style( tween.elem, tween.prop, tween.now + tween.unit );
			} else {
				tween.elem[ tween.prop ] = tween.now;
			}
		}
	}
};

// Support: IE9
// Panic based approach to setting things on disconnected nodes

Tween.propHooks.scrollTop = Tween.propHooks.scrollLeft = {
	set: function( tween ) {
		if ( tween.elem.nodeType && tween.elem.parentNode ) {
			tween.elem[ tween.prop ] = tween.now;
		}
	}
};

jQuery.easing = {
	linear: function( p ) {
		return p;
	},
	swing: function( p ) {
		return 0.5 - Math.cos( p * Math.PI ) / 2;
	}
};

jQuery.fx = Tween.prototype.init;

// Back Compat <1.8 extension point
jQuery.fx.step = {};




var
	fxNow, timerId,
	rfxtypes = /^(?:toggle|show|hide)$/,
	rfxnum = new RegExp( "^(?:([+-])=|)(" + pnum + ")([a-z%]*)$", "i" ),
	rrun = /queueHooks$/,
	animationPrefilters = [ defaultPrefilter ],
	tweeners = {
		"*": [ function( prop, value ) {
			var tween = this.createTween( prop, value ),
				target = tween.cur(),
				parts = rfxnum.exec( value ),
				unit = parts && parts[ 3 ] || ( jQuery.cssNumber[ prop ] ? "" : "px" ),

				// Starting value computation is required for potential unit mismatches
				start = ( jQuery.cssNumber[ prop ] || unit !== "px" && +target ) &&
					rfxnum.exec( jQuery.css( tween.elem, prop ) ),
				scale = 1,
				maxIterations = 20;

			if ( start && start[ 3 ] !== unit ) {
				// Trust units reported by jQuery.css
				unit = unit || start[ 3 ];

				// Make sure we update the tween properties later on
				parts = parts || [];

				// Iteratively approximate from a nonzero starting point
				start = +target || 1;

				do {
					// If previous iteration zeroed out, double until we get *something*
					// Use a string for doubling factor so we don't accidentally see scale as unchanged below
					scale = scale || ".5";

					// Adjust and apply
					start = start / scale;
					jQuery.style( tween.elem, prop, start + unit );

				// Update scale, tolerating zero or NaN from tween.cur()
				// And breaking the loop if scale is unchanged or perfect, or if we've just had enough
				} while ( scale !== (scale = tween.cur() / target) && scale !== 1 && --maxIterations );
			}

			// Update tween properties
			if ( parts ) {
				start = tween.start = +start || +target || 0;
				tween.unit = unit;
				// If a +=/-= token was provided, we're doing a relative animation
				tween.end = parts[ 1 ] ?
					start + ( parts[ 1 ] + 1 ) * parts[ 2 ] :
					+parts[ 2 ];
			}

			return tween;
		} ]
	};

// Animations created synchronously will run synchronously
function createFxNow() {
	setTimeout(function() {
		fxNow = undefined;
	});
	return ( fxNow = jQuery.now() );
}

// Generate parameters to create a standard animation
function genFx( type, includeWidth ) {
	var which,
		i = 0,
		attrs = { height: type };

	// if we include width, step value is 1 to do all cssExpand values,
	// if we don't include width, step value is 2 to skip over Left and Right
	includeWidth = includeWidth ? 1 : 0;
	for ( ; i < 4 ; i += 2 - includeWidth ) {
		which = cssExpand[ i ];
		attrs[ "margin" + which ] = attrs[ "padding" + which ] = type;
	}

	if ( includeWidth ) {
		attrs.opacity = attrs.width = type;
	}

	return attrs;
}

function createTween( value, prop, animation ) {
	var tween,
		collection = ( tweeners[ prop ] || [] ).concat( tweeners[ "*" ] ),
		index = 0,
		length = collection.length;
	for ( ; index < length; index++ ) {
		if ( (tween = collection[ index ].call( animation, prop, value )) ) {

			// we're done with this property
			return tween;
		}
	}
}

function defaultPrefilter( elem, props, opts ) {
	/* jshint validthis: true */
	var prop, value, toggle, tween, hooks, oldfire, display, checkDisplay,
		anim = this,
		orig = {},
		style = elem.style,
		hidden = elem.nodeType && isHidden( elem ),
		dataShow = data_priv.get( elem, "fxshow" );

	// handle queue: false promises
	if ( !opts.queue ) {
		hooks = jQuery._queueHooks( elem, "fx" );
		if ( hooks.unqueued == null ) {
			hooks.unqueued = 0;
			oldfire = hooks.empty.fire;
			hooks.empty.fire = function() {
				if ( !hooks.unqueued ) {
					oldfire();
				}
			};
		}
		hooks.unqueued++;

		anim.always(function() {
			// doing this makes sure that the complete handler will be called
			// before this completes
			anim.always(function() {
				hooks.unqueued--;
				if ( !jQuery.queue( elem, "fx" ).length ) {
					hooks.empty.fire();
				}
			});
		});
	}

	// height/width overflow pass
	if ( elem.nodeType === 1 && ( "height" in props || "width" in props ) ) {
		// Make sure that nothing sneaks out
		// Record all 3 overflow attributes because IE9-10 do not
		// change the overflow attribute when overflowX and
		// overflowY are set to the same value
		opts.overflow = [ style.overflow, style.overflowX, style.overflowY ];

		// Set display property to inline-block for height/width
		// animations on inline elements that are having width/height animated
		display = jQuery.css( elem, "display" );

		// Test default display if display is currently "none"
		checkDisplay = display === "none" ?
			data_priv.get( elem, "olddisplay" ) || defaultDisplay( elem.nodeName ) : display;

		if ( checkDisplay === "inline" && jQuery.css( elem, "float" ) === "none" ) {
			style.display = "inline-block";
		}
	}

	if ( opts.overflow ) {
		style.overflow = "hidden";
		anim.always(function() {
			style.overflow = opts.overflow[ 0 ];
			style.overflowX = opts.overflow[ 1 ];
			style.overflowY = opts.overflow[ 2 ];
		});
	}

	// show/hide pass
	for ( prop in props ) {
		value = props[ prop ];
		if ( rfxtypes.exec( value ) ) {
			delete props[ prop ];
			toggle = toggle || value === "toggle";
			if ( value === ( hidden ? "hide" : "show" ) ) {

				// If there is dataShow left over from a stopped hide or show and we are going to proceed with show, we should pretend to be hidden
				if ( value === "show" && dataShow && dataShow[ prop ] !== undefined ) {
					hidden = true;
				} else {
					continue;
				}
			}
			orig[ prop ] = dataShow && dataShow[ prop ] || jQuery.style( elem, prop );

		// Any non-fx value stops us from restoring the original display value
		} else {
			display = undefined;
		}
	}

	if ( !jQuery.isEmptyObject( orig ) ) {
		if ( dataShow ) {
			if ( "hidden" in dataShow ) {
				hidden = dataShow.hidden;
			}
		} else {
			dataShow = data_priv.access( elem, "fxshow", {} );
		}

		// store state if its toggle - enables .stop().toggle() to "reverse"
		if ( toggle ) {
			dataShow.hidden = !hidden;
		}
		if ( hidden ) {
			jQuery( elem ).show();
		} else {
			anim.done(function() {
				jQuery( elem ).hide();
			});
		}
		anim.done(function() {
			var prop;

			data_priv.remove( elem, "fxshow" );
			for ( prop in orig ) {
				jQuery.style( elem, prop, orig[ prop ] );
			}
		});
		for ( prop in orig ) {
			tween = createTween( hidden ? dataShow[ prop ] : 0, prop, anim );

			if ( !( prop in dataShow ) ) {
				dataShow[ prop ] = tween.start;
				if ( hidden ) {
					tween.end = tween.start;
					tween.start = prop === "width" || prop === "height" ? 1 : 0;
				}
			}
		}

	// If this is a noop like .hide().hide(), restore an overwritten display value
	} else if ( (display === "none" ? defaultDisplay( elem.nodeName ) : display) === "inline" ) {
		style.display = display;
	}
}

function propFilter( props, specialEasing ) {
	var index, name, easing, value, hooks;

	// camelCase, specialEasing and expand cssHook pass
	for ( index in props ) {
		name = jQuery.camelCase( index );
		easing = specialEasing[ name ];
		value = props[ index ];
		if ( jQuery.isArray( value ) ) {
			easing = value[ 1 ];
			value = props[ index ] = value[ 0 ];
		}

		if ( index !== name ) {
			props[ name ] = value;
			delete props[ index ];
		}

		hooks = jQuery.cssHooks[ name ];
		if ( hooks && "expand" in hooks ) {
			value = hooks.expand( value );
			delete props[ name ];

			// not quite $.extend, this wont overwrite keys already present.
			// also - reusing 'index' from above because we have the correct "name"
			for ( index in value ) {
				if ( !( index in props ) ) {
					props[ index ] = value[ index ];
					specialEasing[ index ] = easing;
				}
			}
		} else {
			specialEasing[ name ] = easing;
		}
	}
}

function Animation( elem, properties, options ) {
	var result,
		stopped,
		index = 0,
		length = animationPrefilters.length,
		deferred = jQuery.Deferred().always( function() {
			// don't match elem in the :animated selector
			delete tick.elem;
		}),
		tick = function() {
			if ( stopped ) {
				return false;
			}
			var currentTime = fxNow || createFxNow(),
				remaining = Math.max( 0, animation.startTime + animation.duration - currentTime ),
				// archaic crash bug won't allow us to use 1 - ( 0.5 || 0 ) (#12497)
				temp = remaining / animation.duration || 0,
				percent = 1 - temp,
				index = 0,
				length = animation.tweens.length;

			for ( ; index < length ; index++ ) {
				animation.tweens[ index ].run( percent );
			}

			deferred.notifyWith( elem, [ animation, percent, remaining ]);

			if ( percent < 1 && length ) {
				return remaining;
			} else {
				deferred.resolveWith( elem, [ animation ] );
				return false;
			}
		},
		animation = deferred.promise({
			elem: elem,
			props: jQuery.extend( {}, properties ),
			opts: jQuery.extend( true, { specialEasing: {} }, options ),
			originalProperties: properties,
			originalOptions: options,
			startTime: fxNow || createFxNow(),
			duration: options.duration,
			tweens: [],
			createTween: function( prop, end ) {
				var tween = jQuery.Tween( elem, animation.opts, prop, end,
						animation.opts.specialEasing[ prop ] || animation.opts.easing );
				animation.tweens.push( tween );
				return tween;
			},
			stop: function( gotoEnd ) {
				var index = 0,
					// if we are going to the end, we want to run all the tweens
					// otherwise we skip this part
					length = gotoEnd ? animation.tweens.length : 0;
				if ( stopped ) {
					return this;
				}
				stopped = true;
				for ( ; index < length ; index++ ) {
					animation.tweens[ index ].run( 1 );
				}

				// resolve when we played the last frame
				// otherwise, reject
				if ( gotoEnd ) {
					deferred.resolveWith( elem, [ animation, gotoEnd ] );
				} else {
					deferred.rejectWith( elem, [ animation, gotoEnd ] );
				}
				return this;
			}
		}),
		props = animation.props;

	propFilter( props, animation.opts.specialEasing );

	for ( ; index < length ; index++ ) {
		result = animationPrefilters[ index ].call( animation, elem, props, animation.opts );
		if ( result ) {
			return result;
		}
	}

	jQuery.map( props, createTween, animation );

	if ( jQuery.isFunction( animation.opts.start ) ) {
		animation.opts.start.call( elem, animation );
	}

	jQuery.fx.timer(
		jQuery.extend( tick, {
			elem: elem,
			anim: animation,
			queue: animation.opts.queue
		})
	);

	// attach callbacks from options
	return animation.progress( animation.opts.progress )
		.done( animation.opts.done, animation.opts.complete )
		.fail( animation.opts.fail )
		.always( animation.opts.always );
}

jQuery.Animation = jQuery.extend( Animation, {

	tweener: function( props, callback ) {
		if ( jQuery.isFunction( props ) ) {
			callback = props;
			props = [ "*" ];
		} else {
			props = props.split(" ");
		}

		var prop,
			index = 0,
			length = props.length;

		for ( ; index < length ; index++ ) {
			prop = props[ index ];
			tweeners[ prop ] = tweeners[ prop ] || [];
			tweeners[ prop ].unshift( callback );
		}
	},

	prefilter: function( callback, prepend ) {
		if ( prepend ) {
			animationPrefilters.unshift( callback );
		} else {
			animationPrefilters.push( callback );
		}
	}
});

jQuery.speed = function( speed, easing, fn ) {
	var opt = speed && typeof speed === "object" ? jQuery.extend( {}, speed ) : {
		complete: fn || !fn && easing ||
			jQuery.isFunction( speed ) && speed,
		duration: speed,
		easing: fn && easing || easing && !jQuery.isFunction( easing ) && easing
	};

	opt.duration = jQuery.fx.off ? 0 : typeof opt.duration === "number" ? opt.duration :
		opt.duration in jQuery.fx.speeds ? jQuery.fx.speeds[ opt.duration ] : jQuery.fx.speeds._default;

	// normalize opt.queue - true/undefined/null -> "fx"
	if ( opt.queue == null || opt.queue === true ) {
		opt.queue = "fx";
	}

	// Queueing
	opt.old = opt.complete;

	opt.complete = function() {
		if ( jQuery.isFunction( opt.old ) ) {
			opt.old.call( this );
		}

		if ( opt.queue ) {
			jQuery.dequeue( this, opt.queue );
		}
	};

	return opt;
};

jQuery.fn.extend({
	fadeTo: function( speed, to, easing, callback ) {

		// show any hidden elements after setting opacity to 0
		return this.filter( isHidden ).css( "opacity", 0 ).show()

			// animate to the value specified
			.end().animate({ opacity: to }, speed, easing, callback );
	},
	animate: function( prop, speed, easing, callback ) {
		var empty = jQuery.isEmptyObject( prop ),
			optall = jQuery.speed( speed, easing, callback ),
			doAnimation = function() {
				// Operate on a copy of prop so per-property easing won't be lost
				var anim = Animation( this, jQuery.extend( {}, prop ), optall );

				// Empty animations, or finishing resolves immediately
				if ( empty || data_priv.get( this, "finish" ) ) {
					anim.stop( true );
				}
			};
			doAnimation.finish = doAnimation;

		return empty || optall.queue === false ?
			this.each( doAnimation ) :
			this.queue( optall.queue, doAnimation );
	},
	stop: function( type, clearQueue, gotoEnd ) {
		var stopQueue = function( hooks ) {
			var stop = hooks.stop;
			delete hooks.stop;
			stop( gotoEnd );
		};

		if ( typeof type !== "string" ) {
			gotoEnd = clearQueue;
			clearQueue = type;
			type = undefined;
		}
		if ( clearQueue && type !== false ) {
			this.queue( type || "fx", [] );
		}

		return this.each(function() {
			var dequeue = true,
				index = type != null && type + "queueHooks",
				timers = jQuery.timers,
				data = data_priv.get( this );

			if ( index ) {
				if ( data[ index ] && data[ index ].stop ) {
					stopQueue( data[ index ] );
				}
			} else {
				for ( index in data ) {
					if ( data[ index ] && data[ index ].stop && rrun.test( index ) ) {
						stopQueue( data[ index ] );
					}
				}
			}

			for ( index = timers.length; index--; ) {
				if ( timers[ index ].elem === this && (type == null || timers[ index ].queue === type) ) {
					timers[ index ].anim.stop( gotoEnd );
					dequeue = false;
					timers.splice( index, 1 );
				}
			}

			// start the next in the queue if the last step wasn't forced
			// timers currently will call their complete callbacks, which will dequeue
			// but only if they were gotoEnd
			if ( dequeue || !gotoEnd ) {
				jQuery.dequeue( this, type );
			}
		});
	},
	finish: function( type ) {
		if ( type !== false ) {
			type = type || "fx";
		}
		return this.each(function() {
			var index,
				data = data_priv.get( this ),
				queue = data[ type + "queue" ],
				hooks = data[ type + "queueHooks" ],
				timers = jQuery.timers,
				length = queue ? queue.length : 0;

			// enable finishing flag on private data
			data.finish = true;

			// empty the queue first
			jQuery.queue( this, type, [] );

			if ( hooks && hooks.stop ) {
				hooks.stop.call( this, true );
			}

			// look for any active animations, and finish them
			for ( index = timers.length; index--; ) {
				if ( timers[ index ].elem === this && timers[ index ].queue === type ) {
					timers[ index ].anim.stop( true );
					timers.splice( index, 1 );
				}
			}

			// look for any animations in the old queue and finish them
			for ( index = 0; index < length; index++ ) {
				if ( queue[ index ] && queue[ index ].finish ) {
					queue[ index ].finish.call( this );
				}
			}

			// turn off finishing flag
			delete data.finish;
		});
	}
});

jQuery.each([ "toggle", "show", "hide" ], function( i, name ) {
	var cssFn = jQuery.fn[ name ];
	jQuery.fn[ name ] = function( speed, easing, callback ) {
		return speed == null || typeof speed === "boolean" ?
			cssFn.apply( this, arguments ) :
			this.animate( genFx( name, true ), speed, easing, callback );
	};
});

// Generate shortcuts for custom animations
jQuery.each({
	slideDown: genFx("show"),
	slideUp: genFx("hide"),
	slideToggle: genFx("toggle"),
	fadeIn: { opacity: "show" },
	fadeOut: { opacity: "hide" },
	fadeToggle: { opacity: "toggle" }
}, function( name, props ) {
	jQuery.fn[ name ] = function( speed, easing, callback ) {
		return this.animate( props, speed, easing, callback );
	};
});

jQuery.timers = [];
jQuery.fx.tick = function() {
	var timer,
		i = 0,
		timers = jQuery.timers;

	fxNow = jQuery.now();

	for ( ; i < timers.length; i++ ) {
		timer = timers[ i ];
		// Checks the timer has not already been removed
		if ( !timer() && timers[ i ] === timer ) {
			timers.splice( i--, 1 );
		}
	}

	if ( !timers.length ) {
		jQuery.fx.stop();
	}
	fxNow = undefined;
};

jQuery.fx.timer = function( timer ) {
	jQuery.timers.push( timer );
	if ( timer() ) {
		jQuery.fx.start();
	} else {
		jQuery.timers.pop();
	}
};

jQuery.fx.interval = 13;

jQuery.fx.start = function() {
	if ( !timerId ) {
		timerId = setInterval( jQuery.fx.tick, jQuery.fx.interval );
	}
};

jQuery.fx.stop = function() {
	clearInterval( timerId );
	timerId = null;
};

jQuery.fx.speeds = {
	slow: 600,
	fast: 200,
	// Default speed
	_default: 400
};


// Based off of the plugin by Clint Helfers, with permission.
// http://blindsignals.com/index.php/2009/07/jquery-delay/
jQuery.fn.delay = function( time, type ) {
	time = jQuery.fx ? jQuery.fx.speeds[ time ] || time : time;
	type = type || "fx";

	return this.queue( type, function( next, hooks ) {
		var timeout = setTimeout( next, time );
		hooks.stop = function() {
			clearTimeout( timeout );
		};
	});
};


(function() {
	var input = document.createElement( "input" ),
		select = document.createElement( "select" ),
		opt = select.appendChild( document.createElement( "option" ) );

	input.type = "checkbox";

	// Support: iOS 5.1, Android 4.x, Android 2.3
	// Check the default checkbox/radio value ("" on old WebKit; "on" elsewhere)
	support.checkOn = input.value !== "";

	// Must access the parent to make an option select properly
	// Support: IE9, IE10
	support.optSelected = opt.selected;

	// Make sure that the options inside disabled selects aren't marked as disabled
	// (WebKit marks them as disabled)
	select.disabled = true;
	support.optDisabled = !opt.disabled;

	// Check if an input maintains its value after becoming a radio
	// Support: IE9, IE10
	input = document.createElement( "input" );
	input.value = "t";
	input.type = "radio";
	support.radioValue = input.value === "t";
})();


var nodeHook, boolHook,
	attrHandle = jQuery.expr.attrHandle;

jQuery.fn.extend({
	attr: function( name, value ) {
		return access( this, jQuery.attr, name, value, arguments.length > 1 );
	},

	removeAttr: function( name ) {
		return this.each(function() {
			jQuery.removeAttr( this, name );
		});
	}
});

jQuery.extend({
	attr: function( elem, name, value ) {
		var hooks, ret,
			nType = elem.nodeType;

		// don't get/set attributes on text, comment and attribute nodes
		if ( !elem || nType === 3 || nType === 8 || nType === 2 ) {
			return;
		}

		// Fallback to prop when attributes are not supported
		if ( typeof elem.getAttribute === strundefined ) {
			return jQuery.prop( elem, name, value );
		}

		// All attributes are lowercase
		// Grab necessary hook if one is defined
		if ( nType !== 1 || !jQuery.isXMLDoc( elem ) ) {
			name = name.toLowerCase();
			hooks = jQuery.attrHooks[ name ] ||
				( jQuery.expr.match.bool.test( name ) ? boolHook : nodeHook );
		}

		if ( value !== undefined ) {

			if ( value === null ) {
				jQuery.removeAttr( elem, name );

			} else if ( hooks && "set" in hooks && (ret = hooks.set( elem, value, name )) !== undefined ) {
				return ret;

			} else {
				elem.setAttribute( name, value + "" );
				return value;
			}

		} else if ( hooks && "get" in hooks && (ret = hooks.get( elem, name )) !== null ) {
			return ret;

		} else {
			ret = jQuery.find.attr( elem, name );

			// Non-existent attributes return null, we normalize to undefined
			return ret == null ?
				undefined :
				ret;
		}
	},

	removeAttr: function( elem, value ) {
		var name, propName,
			i = 0,
			attrNames = value && value.match( rnotwhite );

		if ( attrNames && elem.nodeType === 1 ) {
			while ( (name = attrNames[i++]) ) {
				propName = jQuery.propFix[ name ] || name;

				// Boolean attributes get special treatment (#10870)
				if ( jQuery.expr.match.bool.test( name ) ) {
					// Set corresponding property to false
					elem[ propName ] = false;
				}

				elem.removeAttribute( name );
			}
		}
	},

	attrHooks: {
		type: {
			set: function( elem, value ) {
				if ( !support.radioValue && value === "radio" &&
					jQuery.nodeName( elem, "input" ) ) {
					// Setting the type on a radio button after the value resets the value in IE6-9
					// Reset value to default in case type is set after value during creation
					var val = elem.value;
					elem.setAttribute( "type", value );
					if ( val ) {
						elem.value = val;
					}
					return value;
				}
			}
		}
	}
});

// Hooks for boolean attributes
boolHook = {
	set: function( elem, value, name ) {
		if ( value === false ) {
			// Remove boolean attributes when set to false
			jQuery.removeAttr( elem, name );
		} else {
			elem.setAttribute( name, name );
		}
		return name;
	}
};
jQuery.each( jQuery.expr.match.bool.source.match( /\w+/g ), function( i, name ) {
	var getter = attrHandle[ name ] || jQuery.find.attr;

	attrHandle[ name ] = function( elem, name, isXML ) {
		var ret, handle;
		if ( !isXML ) {
			// Avoid an infinite loop by temporarily removing this function from the getter
			handle = attrHandle[ name ];
			attrHandle[ name ] = ret;
			ret = getter( elem, name, isXML ) != null ?
				name.toLowerCase() :
				null;
			attrHandle[ name ] = handle;
		}
		return ret;
	};
});




var rfocusable = /^(?:input|select|textarea|button)$/i;

jQuery.fn.extend({
	prop: function( name, value ) {
		return access( this, jQuery.prop, name, value, arguments.length > 1 );
	},

	removeProp: function( name ) {
		return this.each(function() {
			delete this[ jQuery.propFix[ name ] || name ];
		});
	}
});

jQuery.extend({
	propFix: {
		"for": "htmlFor",
		"class": "className"
	},

	prop: function( elem, name, value ) {
		var ret, hooks, notxml,
			nType = elem.nodeType;

		// don't get/set properties on text, comment and attribute nodes
		if ( !elem || nType === 3 || nType === 8 || nType === 2 ) {
			return;
		}

		notxml = nType !== 1 || !jQuery.isXMLDoc( elem );

		if ( notxml ) {
			// Fix name and attach hooks
			name = jQuery.propFix[ name ] || name;
			hooks = jQuery.propHooks[ name ];
		}

		if ( value !== undefined ) {
			return hooks && "set" in hooks && (ret = hooks.set( elem, value, name )) !== undefined ?
				ret :
				( elem[ name ] = value );

		} else {
			return hooks && "get" in hooks && (ret = hooks.get( elem, name )) !== null ?
				ret :
				elem[ name ];
		}
	},

	propHooks: {
		tabIndex: {
			get: function( elem ) {
				return elem.hasAttribute( "tabindex" ) || rfocusable.test( elem.nodeName ) || elem.href ?
					elem.tabIndex :
					-1;
			}
		}
	}
});

// Support: IE9+
// Selectedness for an option in an optgroup can be inaccurate
if ( !support.optSelected ) {
	jQuery.propHooks.selected = {
		get: function( elem ) {
			var parent = elem.parentNode;
			if ( parent && parent.parentNode ) {
				parent.parentNode.selectedIndex;
			}
			return null;
		}
	};
}

jQuery.each([
	"tabIndex",
	"readOnly",
	"maxLength",
	"cellSpacing",
	"cellPadding",
	"rowSpan",
	"colSpan",
	"useMap",
	"frameBorder",
	"contentEditable"
], function() {
	jQuery.propFix[ this.toLowerCase() ] = this;
});




var rclass = /[\t\r\n\f]/g;

jQuery.fn.extend({
	addClass: function( value ) {
		var classes, elem, cur, clazz, j, finalValue,
			proceed = typeof value === "string" && value,
			i = 0,
			len = this.length;

		if ( jQuery.isFunction( value ) ) {
			return this.each(function( j ) {
				jQuery( this ).addClass( value.call( this, j, this.className ) );
			});
		}

		if ( proceed ) {
			// The disjunction here is for better compressibility (see removeClass)
			classes = ( value || "" ).match( rnotwhite ) || [];

			for ( ; i < len; i++ ) {
				elem = this[ i ];
				cur = elem.nodeType === 1 && ( elem.className ?
					( " " + elem.className + " " ).replace( rclass, " " ) :
					" "
				);

				if ( cur ) {
					j = 0;
					while ( (clazz = classes[j++]) ) {
						if ( cur.indexOf( " " + clazz + " " ) < 0 ) {
							cur += clazz + " ";
						}
					}

					// only assign if different to avoid unneeded rendering.
					finalValue = jQuery.trim( cur );
					if ( elem.className !== finalValue ) {
						elem.className = finalValue;
					}
				}
			}
		}

		return this;
	},

	removeClass: function( value ) {
		var classes, elem, cur, clazz, j, finalValue,
			proceed = arguments.length === 0 || typeof value === "string" && value,
			i = 0,
			len = this.length;

		if ( jQuery.isFunction( value ) ) {
			return this.each(function( j ) {
				jQuery( this ).removeClass( value.call( this, j, this.className ) );
			});
		}
		if ( proceed ) {
			classes = ( value || "" ).match( rnotwhite ) || [];

			for ( ; i < len; i++ ) {
				elem = this[ i ];
				// This expression is here for better compressibility (see addClass)
				cur = elem.nodeType === 1 && ( elem.className ?
					( " " + elem.className + " " ).replace( rclass, " " ) :
					""
				);

				if ( cur ) {
					j = 0;
					while ( (clazz = classes[j++]) ) {
						// Remove *all* instances
						while ( cur.indexOf( " " + clazz + " " ) >= 0 ) {
							cur = cur.replace( " " + clazz + " ", " " );
						}
					}

					// only assign if different to avoid unneeded rendering.
					finalValue = value ? jQuery.trim( cur ) : "";
					if ( elem.className !== finalValue ) {
						elem.className = finalValue;
					}
				}
			}
		}

		return this;
	},

	toggleClass: function( value, stateVal ) {
		var type = typeof value;

		if ( typeof stateVal === "boolean" && type === "string" ) {
			return stateVal ? this.addClass( value ) : this.removeClass( value );
		}

		if ( jQuery.isFunction( value ) ) {
			return this.each(function( i ) {
				jQuery( this ).toggleClass( value.call(this, i, this.className, stateVal), stateVal );
			});
		}

		return this.each(function() {
			if ( type === "string" ) {
				// toggle individual class names
				var className,
					i = 0,
					self = jQuery( this ),
					classNames = value.match( rnotwhite ) || [];

				while ( (className = classNames[ i++ ]) ) {
					// check each className given, space separated list
					if ( self.hasClass( className ) ) {
						self.removeClass( className );
					} else {
						self.addClass( className );
					}
				}

			// Toggle whole class name
			} else if ( type === strundefined || type === "boolean" ) {
				if ( this.className ) {
					// store className if set
					data_priv.set( this, "__className__", this.className );
				}

				// If the element has a class name or if we're passed "false",
				// then remove the whole classname (if there was one, the above saved it).
				// Otherwise bring back whatever was previously saved (if anything),
				// falling back to the empty string if nothing was stored.
				this.className = this.className || value === false ? "" : data_priv.get( this, "__className__" ) || "";
			}
		});
	},

	hasClass: function( selector ) {
		var className = " " + selector + " ",
			i = 0,
			l = this.length;
		for ( ; i < l; i++ ) {
			if ( this[i].nodeType === 1 && (" " + this[i].className + " ").replace(rclass, " ").indexOf( className ) >= 0 ) {
				return true;
			}
		}

		return false;
	}
});




var rreturn = /\r/g;

jQuery.fn.extend({
	val: function( value ) {
		var hooks, ret, isFunction,
			elem = this[0];

		if ( !arguments.length ) {
			if ( elem ) {
				hooks = jQuery.valHooks[ elem.type ] || jQuery.valHooks[ elem.nodeName.toLowerCase() ];

				if ( hooks && "get" in hooks && (ret = hooks.get( elem, "value" )) !== undefined ) {
					return ret;
				}

				ret = elem.value;

				return typeof ret === "string" ?
					// handle most common string cases
					ret.replace(rreturn, "") :
					// handle cases where value is null/undef or number
					ret == null ? "" : ret;
			}

			return;
		}

		isFunction = jQuery.isFunction( value );

		return this.each(function( i ) {
			var val;

			if ( this.nodeType !== 1 ) {
				return;
			}

			if ( isFunction ) {
				val = value.call( this, i, jQuery( this ).val() );
			} else {
				val = value;
			}

			// Treat null/undefined as ""; convert numbers to string
			if ( val == null ) {
				val = "";

			} else if ( typeof val === "number" ) {
				val += "";

			} else if ( jQuery.isArray( val ) ) {
				val = jQuery.map( val, function( value ) {
					return value == null ? "" : value + "";
				});
			}

			hooks = jQuery.valHooks[ this.type ] || jQuery.valHooks[ this.nodeName.toLowerCase() ];

			// If set returns undefined, fall back to normal setting
			if ( !hooks || !("set" in hooks) || hooks.set( this, val, "value" ) === undefined ) {
				this.value = val;
			}
		});
	}
});

jQuery.extend({
	valHooks: {
		option: {
			get: function( elem ) {
				var val = jQuery.find.attr( elem, "value" );
				return val != null ?
					val :
					// Support: IE10-11+
					// option.text throws exceptions (#14686, #14858)
					jQuery.trim( jQuery.text( elem ) );
			}
		},
		select: {
			get: function( elem ) {
				var value, option,
					options = elem.options,
					index = elem.selectedIndex,
					one = elem.type === "select-one" || index < 0,
					values = one ? null : [],
					max = one ? index + 1 : options.length,
					i = index < 0 ?
						max :
						one ? index : 0;

				// Loop through all the selected options
				for ( ; i < max; i++ ) {
					option = options[ i ];

					// IE6-9 doesn't update selected after form reset (#2551)
					if ( ( option.selected || i === index ) &&
							// Don't return options that are disabled or in a disabled optgroup
							( support.optDisabled ? !option.disabled : option.getAttribute( "disabled" ) === null ) &&
							( !option.parentNode.disabled || !jQuery.nodeName( option.parentNode, "optgroup" ) ) ) {

						// Get the specific value for the option
						value = jQuery( option ).val();

						// We don't need an array for one selects
						if ( one ) {
							return value;
						}

						// Multi-Selects return an array
						values.push( value );
					}
				}

				return values;
			},

			set: function( elem, value ) {
				var optionSet, option,
					options = elem.options,
					values = jQuery.makeArray( value ),
					i = options.length;

				while ( i-- ) {
					option = options[ i ];
					if ( (option.selected = jQuery.inArray( option.value, values ) >= 0) ) {
						optionSet = true;
					}
				}

				// force browsers to behave consistently when non-matching value is set
				if ( !optionSet ) {
					elem.selectedIndex = -1;
				}
				return values;
			}
		}
	}
});

// Radios and checkboxes getter/setter
jQuery.each([ "radio", "checkbox" ], function() {
	jQuery.valHooks[ this ] = {
		set: function( elem, value ) {
			if ( jQuery.isArray( value ) ) {
				return ( elem.checked = jQuery.inArray( jQuery(elem).val(), value ) >= 0 );
			}
		}
	};
	if ( !support.checkOn ) {
		jQuery.valHooks[ this ].get = function( elem ) {
			// Support: Webkit
			// "" is returned instead of "on" if a value isn't specified
			return elem.getAttribute("value") === null ? "on" : elem.value;
		};
	}
});




// Return jQuery for attributes-only inclusion


jQuery.each( ("blur focus focusin focusout load resize scroll unload click dblclick " +
	"mousedown mouseup mousemove mouseover mouseout mouseenter mouseleave " +
	"change select submit keydown keypress keyup error contextmenu").split(" "), function( i, name ) {

	// Handle event binding
	jQuery.fn[ name ] = function( data, fn ) {
		return arguments.length > 0 ?
			this.on( name, null, data, fn ) :
			this.trigger( name );
	};
});

jQuery.fn.extend({
	hover: function( fnOver, fnOut ) {
		return this.mouseenter( fnOver ).mouseleave( fnOut || fnOver );
	},

	bind: function( types, data, fn ) {
		return this.on( types, null, data, fn );
	},
	unbind: function( types, fn ) {
		return this.off( types, null, fn );
	},

	delegate: function( selector, types, data, fn ) {
		return this.on( types, selector, data, fn );
	},
	undelegate: function( selector, types, fn ) {
		// ( namespace ) or ( selector, types [, fn] )
		return arguments.length === 1 ? this.off( selector, "**" ) : this.off( types, selector || "**", fn );
	}
});


var nonce = jQuery.now();

var rquery = (/\?/);



// Support: Android 2.3
// Workaround failure to string-cast null input
jQuery.parseJSON = function( data ) {
	return JSON.parse( data + "" );
};


// Cross-browser xml parsing
jQuery.parseXML = function( data ) {
	var xml, tmp;
	if ( !data || typeof data !== "string" ) {
		return null;
	}

	// Support: IE9
	try {
		tmp = new DOMParser();
		xml = tmp.parseFromString( data, "text/xml" );
	} catch ( e ) {
		xml = undefined;
	}

	if ( !xml || xml.getElementsByTagName( "parsererror" ).length ) {
		jQuery.error( "Invalid XML: " + data );
	}
	return xml;
};


var
	// Document location
	ajaxLocParts,
	ajaxLocation,

	rhash = /#.*$/,
	rts = /([?&])_=[^&]*/,
	rheaders = /^(.*?):[ \t]*([^\r\n]*)$/mg,
	// #7653, #8125, #8152: local protocol detection
	rlocalProtocol = /^(?:about|app|app-storage|.+-extension|file|res|widget):$/,
	rnoContent = /^(?:GET|HEAD)$/,
	rprotocol = /^\/\//,
	rurl = /^([\w.+-]+:)(?:\/\/(?:[^\/?#]*@|)([^\/?#:]*)(?::(\d+)|)|)/,

	/* Prefilters
	 * 1) They are useful to introduce custom dataTypes (see ajax/jsonp.js for an example)
	 * 2) These are called:
	 *    - BEFORE asking for a transport
	 *    - AFTER param serialization (s.data is a string if s.processData is true)
	 * 3) key is the dataType
	 * 4) the catchall symbol "*" can be used
	 * 5) execution will start with transport dataType and THEN continue down to "*" if needed
	 */
	prefilters = {},

	/* Transports bindings
	 * 1) key is the dataType
	 * 2) the catchall symbol "*" can be used
	 * 3) selection will start with transport dataType and THEN go to "*" if needed
	 */
	transports = {},

	// Avoid comment-prolog char sequence (#10098); must appease lint and evade compression
	allTypes = "*/".concat("*");

// #8138, IE may throw an exception when accessing
// a field from window.location if document.domain has been set
try {
	ajaxLocation = location.href;
} catch( e ) {
	// Use the href attribute of an A element
	// since IE will modify it given document.location
	ajaxLocation = document.createElement( "a" );
	ajaxLocation.href = "";
	ajaxLocation = ajaxLocation.href;
}

// Segment location into parts
ajaxLocParts = rurl.exec( ajaxLocation.toLowerCase() ) || [];

// Base "constructor" for jQuery.ajaxPrefilter and jQuery.ajaxTransport
function addToPrefiltersOrTransports( structure ) {

	// dataTypeExpression is optional and defaults to "*"
	return function( dataTypeExpression, func ) {

		if ( typeof dataTypeExpression !== "string" ) {
			func = dataTypeExpression;
			dataTypeExpression = "*";
		}

		var dataType,
			i = 0,
			dataTypes = dataTypeExpression.toLowerCase().match( rnotwhite ) || [];

		if ( jQuery.isFunction( func ) ) {
			// For each dataType in the dataTypeExpression
			while ( (dataType = dataTypes[i++]) ) {
				// Prepend if requested
				if ( dataType[0] === "+" ) {
					dataType = dataType.slice( 1 ) || "*";
					(structure[ dataType ] = structure[ dataType ] || []).unshift( func );

				// Otherwise append
				} else {
					(structure[ dataType ] = structure[ dataType ] || []).push( func );
				}
			}
		}
	};
}

// Base inspection function for prefilters and transports
function inspectPrefiltersOrTransports( structure, options, originalOptions, jqXHR ) {

	var inspected = {},
		seekingTransport = ( structure === transports );

	function inspect( dataType ) {
		var selected;
		inspected[ dataType ] = true;
		jQuery.each( structure[ dataType ] || [], function( _, prefilterOrFactory ) {
			var dataTypeOrTransport = prefilterOrFactory( options, originalOptions, jqXHR );
			if ( typeof dataTypeOrTransport === "string" && !seekingTransport && !inspected[ dataTypeOrTransport ] ) {
				options.dataTypes.unshift( dataTypeOrTransport );
				inspect( dataTypeOrTransport );
				return false;
			} else if ( seekingTransport ) {
				return !( selected = dataTypeOrTransport );
			}
		});
		return selected;
	}

	return inspect( options.dataTypes[ 0 ] ) || !inspected[ "*" ] && inspect( "*" );
}

// A special extend for ajax options
// that takes "flat" options (not to be deep extended)
// Fixes #9887
function ajaxExtend( target, src ) {
	var key, deep,
		flatOptions = jQuery.ajaxSettings.flatOptions || {};

	for ( key in src ) {
		if ( src[ key ] !== undefined ) {
			( flatOptions[ key ] ? target : ( deep || (deep = {}) ) )[ key ] = src[ key ];
		}
	}
	if ( deep ) {
		jQuery.extend( true, target, deep );
	}

	return target;
}

/* Handles responses to an ajax request:
 * - finds the right dataType (mediates between content-type and expected dataType)
 * - returns the corresponding response
 */
function ajaxHandleResponses( s, jqXHR, responses ) {

	var ct, type, finalDataType, firstDataType,
		contents = s.contents,
		dataTypes = s.dataTypes;

	// Remove auto dataType and get content-type in the process
	while ( dataTypes[ 0 ] === "*" ) {
		dataTypes.shift();
		if ( ct === undefined ) {
			ct = s.mimeType || jqXHR.getResponseHeader("Content-Type");
		}
	}

	// Check if we're dealing with a known content-type
	if ( ct ) {
		for ( type in contents ) {
			if ( contents[ type ] && contents[ type ].test( ct ) ) {
				dataTypes.unshift( type );
				break;
			}
		}
	}

	// Check to see if we have a response for the expected dataType
	if ( dataTypes[ 0 ] in responses ) {
		finalDataType = dataTypes[ 0 ];
	} else {
		// Try convertible dataTypes
		for ( type in responses ) {
			if ( !dataTypes[ 0 ] || s.converters[ type + " " + dataTypes[0] ] ) {
				finalDataType = type;
				break;
			}
			if ( !firstDataType ) {
				firstDataType = type;
			}
		}
		// Or just use first one
		finalDataType = finalDataType || firstDataType;
	}

	// If we found a dataType
	// We add the dataType to the list if needed
	// and return the corresponding response
	if ( finalDataType ) {
		if ( finalDataType !== dataTypes[ 0 ] ) {
			dataTypes.unshift( finalDataType );
		}
		return responses[ finalDataType ];
	}
}

/* Chain conversions given the request and the original response
 * Also sets the responseXXX fields on the jqXHR instance
 */
function ajaxConvert( s, response, jqXHR, isSuccess ) {
	var conv2, current, conv, tmp, prev,
		converters = {},
		// Work with a copy of dataTypes in case we need to modify it for conversion
		dataTypes = s.dataTypes.slice();

	// Create converters map with lowercased keys
	if ( dataTypes[ 1 ] ) {
		for ( conv in s.converters ) {
			converters[ conv.toLowerCase() ] = s.converters[ conv ];
		}
	}

	current = dataTypes.shift();

	// Convert to each sequential dataType
	while ( current ) {

		if ( s.responseFields[ current ] ) {
			jqXHR[ s.responseFields[ current ] ] = response;
		}

		// Apply the dataFilter if provided
		if ( !prev && isSuccess && s.dataFilter ) {
			response = s.dataFilter( response, s.dataType );
		}

		prev = current;
		current = dataTypes.shift();

		if ( current ) {

		// There's only work to do if current dataType is non-auto
			if ( current === "*" ) {

				current = prev;

			// Convert response if prev dataType is non-auto and differs from current
			} else if ( prev !== "*" && prev !== current ) {

				// Seek a direct converter
				conv = converters[ prev + " " + current ] || converters[ "* " + current ];

				// If none found, seek a pair
				if ( !conv ) {
					for ( conv2 in converters ) {

						// If conv2 outputs current
						tmp = conv2.split( " " );
						if ( tmp[ 1 ] === current ) {

							// If prev can be converted to accepted input
							conv = converters[ prev + " " + tmp[ 0 ] ] ||
								converters[ "* " + tmp[ 0 ] ];
							if ( conv ) {
								// Condense equivalence converters
								if ( conv === true ) {
									conv = converters[ conv2 ];

								// Otherwise, insert the intermediate dataType
								} else if ( converters[ conv2 ] !== true ) {
									current = tmp[ 0 ];
									dataTypes.unshift( tmp[ 1 ] );
								}
								break;
							}
						}
					}
				}

				// Apply converter (if not an equivalence)
				if ( conv !== true ) {

					// Unless errors are allowed to bubble, catch and return them
					if ( conv && s[ "throws" ] ) {
						response = conv( response );
					} else {
						try {
							response = conv( response );
						} catch ( e ) {
							return { state: "parsererror", error: conv ? e : "No conversion from " + prev + " to " + current };
						}
					}
				}
			}
		}
	}

	return { state: "success", data: response };
}

jQuery.extend({

	// Counter for holding the number of active queries
	active: 0,

	// Last-Modified header cache for next request
	lastModified: {},
	etag: {},

	ajaxSettings: {
		url: ajaxLocation,
		type: "GET",
		isLocal: rlocalProtocol.test( ajaxLocParts[ 1 ] ),
		global: true,
		processData: true,
		async: true,
		contentType: "application/x-www-form-urlencoded; charset=UTF-8",
		/*
		timeout: 0,
		data: null,
		dataType: null,
		username: null,
		password: null,
		cache: null,
		throws: false,
		traditional: false,
		headers: {},
		*/

		accepts: {
			"*": allTypes,
			text: "text/plain",
			html: "text/html",
			xml: "application/xml, text/xml",
			json: "application/json, text/javascript"
		},

		contents: {
			xml: /xml/,
			html: /html/,
			json: /json/
		},

		responseFields: {
			xml: "responseXML",
			text: "responseText",
			json: "responseJSON"
		},

		// Data converters
		// Keys separate source (or catchall "*") and destination types with a single space
		converters: {

			// Convert anything to text
			"* text": String,

			// Text to html (true = no transformation)
			"text html": true,

			// Evaluate text as a json expression
			"text json": jQuery.parseJSON,

			// Parse text as xml
			"text xml": jQuery.parseXML
		},

		// For options that shouldn't be deep extended:
		// you can add your own custom options here if
		// and when you create one that shouldn't be
		// deep extended (see ajaxExtend)
		flatOptions: {
			url: true,
			context: true
		}
	},

	// Creates a full fledged settings object into target
	// with both ajaxSettings and settings fields.
	// If target is omitted, writes into ajaxSettings.
	ajaxSetup: function( target, settings ) {
		return settings ?

			// Building a settings object
			ajaxExtend( ajaxExtend( target, jQuery.ajaxSettings ), settings ) :

			// Extending ajaxSettings
			ajaxExtend( jQuery.ajaxSettings, target );
	},

	ajaxPrefilter: addToPrefiltersOrTransports( prefilters ),
	ajaxTransport: addToPrefiltersOrTransports( transports ),

	// Main method
	ajax: function( url, options ) {

		// If url is an object, simulate pre-1.5 signature
		if ( typeof url === "object" ) {
			options = url;
			url = undefined;
		}

		// Force options to be an object
		options = options || {};

		var transport,
			// URL without anti-cache param
			cacheURL,
			// Response headers
			responseHeadersString,
			responseHeaders,
			// timeout handle
			timeoutTimer,
			// Cross-domain detection vars
			parts,
			// To know if global events are to be dispatched
			fireGlobals,
			// Loop variable
			i,
			// Create the final options object
			s = jQuery.ajaxSetup( {}, options ),
			// Callbacks context
			callbackContext = s.context || s,
			// Context for global events is callbackContext if it is a DOM node or jQuery collection
			globalEventContext = s.context && ( callbackContext.nodeType || callbackContext.jquery ) ?
				jQuery( callbackContext ) :
				jQuery.event,
			// Deferreds
			deferred = jQuery.Deferred(),
			completeDeferred = jQuery.Callbacks("once memory"),
			// Status-dependent callbacks
			statusCode = s.statusCode || {},
			// Headers (they are sent all at once)
			requestHeaders = {},
			requestHeadersNames = {},
			// The jqXHR state
			state = 0,
			// Default abort message
			strAbort = "canceled",
			// Fake xhr
			jqXHR = {
				readyState: 0,

				// Builds headers hashtable if needed
				getResponseHeader: function( key ) {
					var match;
					if ( state === 2 ) {
						if ( !responseHeaders ) {
							responseHeaders = {};
							while ( (match = rheaders.exec( responseHeadersString )) ) {
								responseHeaders[ match[1].toLowerCase() ] = match[ 2 ];
							}
						}
						match = responseHeaders[ key.toLowerCase() ];
					}
					return match == null ? null : match;
				},

				// Raw string
				getAllResponseHeaders: function() {
					return state === 2 ? responseHeadersString : null;
				},

				// Caches the header
				setRequestHeader: function( name, value ) {
					var lname = name.toLowerCase();
					if ( !state ) {
						name = requestHeadersNames[ lname ] = requestHeadersNames[ lname ] || name;
						requestHeaders[ name ] = value;
					}
					return this;
				},

				// Overrides response content-type header
				overrideMimeType: function( type ) {
					if ( !state ) {
						s.mimeType = type;
					}
					return this;
				},

				// Status-dependent callbacks
				statusCode: function( map ) {
					var code;
					if ( map ) {
						if ( state < 2 ) {
							for ( code in map ) {
								// Lazy-add the new callback in a way that preserves old ones
								statusCode[ code ] = [ statusCode[ code ], map[ code ] ];
							}
						} else {
							// Execute the appropriate callbacks
							jqXHR.always( map[ jqXHR.status ] );
						}
					}
					return this;
				},

				// Cancel the request
				abort: function( statusText ) {
					var finalText = statusText || strAbort;
					if ( transport ) {
						transport.abort( finalText );
					}
					done( 0, finalText );
					return this;
				}
			};

		// Attach deferreds
		deferred.promise( jqXHR ).complete = completeDeferred.add;
		jqXHR.success = jqXHR.done;
		jqXHR.error = jqXHR.fail;

		// Remove hash character (#7531: and string promotion)
		// Add protocol if not provided (prefilters might expect it)
		// Handle falsy url in the settings object (#10093: consistency with old signature)
		// We also use the url parameter if available
		s.url = ( ( url || s.url || ajaxLocation ) + "" ).replace( rhash, "" )
			.replace( rprotocol, ajaxLocParts[ 1 ] + "//" );

		// Alias method option to type as per ticket #12004
		s.type = options.method || options.type || s.method || s.type;

		// Extract dataTypes list
		s.dataTypes = jQuery.trim( s.dataType || "*" ).toLowerCase().match( rnotwhite ) || [ "" ];

		// A cross-domain request is in order when we have a protocol:host:port mismatch
		if ( s.crossDomain == null ) {
			parts = rurl.exec( s.url.toLowerCase() );
			s.crossDomain = !!( parts &&
				( parts[ 1 ] !== ajaxLocParts[ 1 ] || parts[ 2 ] !== ajaxLocParts[ 2 ] ||
					( parts[ 3 ] || ( parts[ 1 ] === "http:" ? "80" : "443" ) ) !==
						( ajaxLocParts[ 3 ] || ( ajaxLocParts[ 1 ] === "http:" ? "80" : "443" ) ) )
			);
		}

		// Convert data if not already a string
		if ( s.data && s.processData && typeof s.data !== "string" ) {
			s.data = jQuery.param( s.data, s.traditional );
		}

		// Apply prefilters
		inspectPrefiltersOrTransports( prefilters, s, options, jqXHR );

		// If request was aborted inside a prefilter, stop there
		if ( state === 2 ) {
			return jqXHR;
		}

		// We can fire global events as of now if asked to
		fireGlobals = s.global;

		// Watch for a new set of requests
		if ( fireGlobals && jQuery.active++ === 0 ) {
			jQuery.event.trigger("ajaxStart");
		}

		// Uppercase the type
		s.type = s.type.toUpperCase();

		// Determine if request has content
		s.hasContent = !rnoContent.test( s.type );

		// Save the URL in case we're toying with the If-Modified-Since
		// and/or If-None-Match header later on
		cacheURL = s.url;

		// More options handling for requests with no content
		if ( !s.hasContent ) {

			// If data is available, append data to url
			if ( s.data ) {
				cacheURL = ( s.url += ( rquery.test( cacheURL ) ? "&" : "?" ) + s.data );
				// #9682: remove data so that it's not used in an eventual retry
				delete s.data;
			}

			// Add anti-cache in url if needed
			if ( s.cache === false ) {
				s.url = rts.test( cacheURL ) ?

					// If there is already a '_' parameter, set its value
					cacheURL.replace( rts, "$1_=" + nonce++ ) :

					// Otherwise add one to the end
					cacheURL + ( rquery.test( cacheURL ) ? "&" : "?" ) + "_=" + nonce++;
			}
		}

		// Set the If-Modified-Since and/or If-None-Match header, if in ifModified mode.
		if ( s.ifModified ) {
			if ( jQuery.lastModified[ cacheURL ] ) {
				jqXHR.setRequestHeader( "If-Modified-Since", jQuery.lastModified[ cacheURL ] );
			}
			if ( jQuery.etag[ cacheURL ] ) {
				jqXHR.setRequestHeader( "If-None-Match", jQuery.etag[ cacheURL ] );
			}
		}

		// Set the correct header, if data is being sent
		if ( s.data && s.hasContent && s.contentType !== false || options.contentType ) {
			jqXHR.setRequestHeader( "Content-Type", s.contentType );
		}

		// Set the Accepts header for the server, depending on the dataType
		jqXHR.setRequestHeader(
			"Accept",
			s.dataTypes[ 0 ] && s.accepts[ s.dataTypes[0] ] ?
				s.accepts[ s.dataTypes[0] ] + ( s.dataTypes[ 0 ] !== "*" ? ", " + allTypes + "; q=0.01" : "" ) :
				s.accepts[ "*" ]
		);

		// Check for headers option
		for ( i in s.headers ) {
			jqXHR.setRequestHeader( i, s.headers[ i ] );
		}

		// Allow custom headers/mimetypes and early abort
		if ( s.beforeSend && ( s.beforeSend.call( callbackContext, jqXHR, s ) === false || state === 2 ) ) {
			// Abort if not done already and return
			return jqXHR.abort();
		}

		// aborting is no longer a cancellation
		strAbort = "abort";

		// Install callbacks on deferreds
		for ( i in { success: 1, error: 1, complete: 1 } ) {
			jqXHR[ i ]( s[ i ] );
		}

		// Get transport
		transport = inspectPrefiltersOrTransports( transports, s, options, jqXHR );

		// If no transport, we auto-abort
		if ( !transport ) {
			done( -1, "No Transport" );
		} else {
			jqXHR.readyState = 1;

			// Send global event
			if ( fireGlobals ) {
				globalEventContext.trigger( "ajaxSend", [ jqXHR, s ] );
			}
			// Timeout
			if ( s.async && s.timeout > 0 ) {
				timeoutTimer = setTimeout(function() {
					jqXHR.abort("timeout");
				}, s.timeout );
			}

			try {
				state = 1;
				transport.send( requestHeaders, done );
			} catch ( e ) {
				// Propagate exception as error if not done
				if ( state < 2 ) {
					done( -1, e );
				// Simply rethrow otherwise
				} else {
					throw e;
				}
			}
		}

		// Callback for when everything is done
		function done( status, nativeStatusText, responses, headers ) {
			var isSuccess, success, error, response, modified,
				statusText = nativeStatusText;

			// Called once
			if ( state === 2 ) {
				return;
			}

			// State is "done" now
			state = 2;

			// Clear timeout if it exists
			if ( timeoutTimer ) {
				clearTimeout( timeoutTimer );
			}

			// Dereference transport for early garbage collection
			// (no matter how long the jqXHR object will be used)
			transport = undefined;

			// Cache response headers
			responseHeadersString = headers || "";

			// Set readyState
			jqXHR.readyState = status > 0 ? 4 : 0;

			// Determine if successful
			isSuccess = status >= 200 && status < 300 || status === 304;

			// Get response data
			if ( responses ) {
				response = ajaxHandleResponses( s, jqXHR, responses );
			}

			// Convert no matter what (that way responseXXX fields are always set)
			response = ajaxConvert( s, response, jqXHR, isSuccess );

			// If successful, handle type chaining
			if ( isSuccess ) {

				// Set the If-Modified-Since and/or If-None-Match header, if in ifModified mode.
				if ( s.ifModified ) {
					modified = jqXHR.getResponseHeader("Last-Modified");
					if ( modified ) {
						jQuery.lastModified[ cacheURL ] = modified;
					}
					modified = jqXHR.getResponseHeader("etag");
					if ( modified ) {
						jQuery.etag[ cacheURL ] = modified;
					}
				}

				// if no content
				if ( status === 204 || s.type === "HEAD" ) {
					statusText = "nocontent";

				// if not modified
				} else if ( status === 304 ) {
					statusText = "notmodified";

				// If we have data, let's convert it
				} else {
					statusText = response.state;
					success = response.data;
					error = response.error;
					isSuccess = !error;
				}
			} else {
				// We extract error from statusText
				// then normalize statusText and status for non-aborts
				error = statusText;
				if ( status || !statusText ) {
					statusText = "error";
					if ( status < 0 ) {
						status = 0;
					}
				}
			}

			// Set data for the fake xhr object
			jqXHR.status = status;
			jqXHR.statusText = ( nativeStatusText || statusText ) + "";

			// Success/Error
			if ( isSuccess ) {
				deferred.resolveWith( callbackContext, [ success, statusText, jqXHR ] );
			} else {
				deferred.rejectWith( callbackContext, [ jqXHR, statusText, error ] );
			}

			// Status-dependent callbacks
			jqXHR.statusCode( statusCode );
			statusCode = undefined;

			if ( fireGlobals ) {
				globalEventContext.trigger( isSuccess ? "ajaxSuccess" : "ajaxError",
					[ jqXHR, s, isSuccess ? success : error ] );
			}

			// Complete
			completeDeferred.fireWith( callbackContext, [ jqXHR, statusText ] );

			if ( fireGlobals ) {
				globalEventContext.trigger( "ajaxComplete", [ jqXHR, s ] );
				// Handle the global AJAX counter
				if ( !( --jQuery.active ) ) {
					jQuery.event.trigger("ajaxStop");
				}
			}
		}

		return jqXHR;
	},

	getJSON: function( url, data, callback ) {
		return jQuery.get( url, data, callback, "json" );
	},

	getScript: function( url, callback ) {
		return jQuery.get( url, undefined, callback, "script" );
	}
});

jQuery.each( [ "get", "post" ], function( i, method ) {
	jQuery[ method ] = function( url, data, callback, type ) {
		// shift arguments if data argument was omitted
		if ( jQuery.isFunction( data ) ) {
			type = type || callback;
			callback = data;
			data = undefined;
		}

		return jQuery.ajax({
			url: url,
			type: method,
			dataType: type,
			data: data,
			success: callback
		});
	};
});

// Attach a bunch of functions for handling common AJAX events
jQuery.each( [ "ajaxStart", "ajaxStop", "ajaxComplete", "ajaxError", "ajaxSuccess", "ajaxSend" ], function( i, type ) {
	jQuery.fn[ type ] = function( fn ) {
		return this.on( type, fn );
	};
});


jQuery._evalUrl = function( url ) {
	return jQuery.ajax({
		url: url,
		type: "GET",
		dataType: "script",
		async: false,
		global: false,
		"throws": true
	});
};


jQuery.fn.extend({
	wrapAll: function( html ) {
		var wrap;

		if ( jQuery.isFunction( html ) ) {
			return this.each(function( i ) {
				jQuery( this ).wrapAll( html.call(this, i) );
			});
		}

		if ( this[ 0 ] ) {

			// The elements to wrap the target around
			wrap = jQuery( html, this[ 0 ].ownerDocument ).eq( 0 ).clone( true );

			if ( this[ 0 ].parentNode ) {
				wrap.insertBefore( this[ 0 ] );
			}

			wrap.map(function() {
				var elem = this;

				while ( elem.firstElementChild ) {
					elem = elem.firstElementChild;
				}

				return elem;
			}).append( this );
		}

		return this;
	},

	wrapInner: function( html ) {
		if ( jQuery.isFunction( html ) ) {
			return this.each(function( i ) {
				jQuery( this ).wrapInner( html.call(this, i) );
			});
		}

		return this.each(function() {
			var self = jQuery( this ),
				contents = self.contents();

			if ( contents.length ) {
				contents.wrapAll( html );

			} else {
				self.append( html );
			}
		});
	},

	wrap: function( html ) {
		var isFunction = jQuery.isFunction( html );

		return this.each(function( i ) {
			jQuery( this ).wrapAll( isFunction ? html.call(this, i) : html );
		});
	},

	unwrap: function() {
		return this.parent().each(function() {
			if ( !jQuery.nodeName( this, "body" ) ) {
				jQuery( this ).replaceWith( this.childNodes );
			}
		}).end();
	}
});


jQuery.expr.filters.hidden = function( elem ) {
	// Support: Opera <= 12.12
	// Opera reports offsetWidths and offsetHeights less than zero on some elements
	return elem.offsetWidth <= 0 && elem.offsetHeight <= 0;
};
jQuery.expr.filters.visible = function( elem ) {
	return !jQuery.expr.filters.hidden( elem );
};




var r20 = /%20/g,
	rbracket = /\[\]$/,
	rCRLF = /\r?\n/g,
	rsubmitterTypes = /^(?:submit|button|image|reset|file)$/i,
	rsubmittable = /^(?:input|select|textarea|keygen)/i;

function buildParams( prefix, obj, traditional, add ) {
	var name;

	if ( jQuery.isArray( obj ) ) {
		// Serialize array item.
		jQuery.each( obj, function( i, v ) {
			if ( traditional || rbracket.test( prefix ) ) {
				// Treat each array item as a scalar.
				add( prefix, v );

			} else {
				// Item is non-scalar (array or object), encode its numeric index.
				buildParams( prefix + "[" + ( typeof v === "object" ? i : "" ) + "]", v, traditional, add );
			}
		});

	} else if ( !traditional && jQuery.type( obj ) === "object" ) {
		// Serialize object item.
		for ( name in obj ) {
			buildParams( prefix + "[" + name + "]", obj[ name ], traditional, add );
		}

	} else {
		// Serialize scalar item.
		add( prefix, obj );
	}
}

// Serialize an array of form elements or a set of
// key/values into a query string
jQuery.param = function( a, traditional ) {
	var prefix,
		s = [],
		add = function( key, value ) {
			// If value is a function, invoke it and return its value
			value = jQuery.isFunction( value ) ? value() : ( value == null ? "" : value );
			s[ s.length ] = encodeURIComponent( key ) + "=" + encodeURIComponent( value );
		};

	// Set traditional to true for jQuery <= 1.3.2 behavior.
	if ( traditional === undefined ) {
		traditional = jQuery.ajaxSettings && jQuery.ajaxSettings.traditional;
	}

	// If an array was passed in, assume that it is an array of form elements.
	if ( jQuery.isArray( a ) || ( a.jquery && !jQuery.isPlainObject( a ) ) ) {
		// Serialize the form elements
		jQuery.each( a, function() {
			add( this.name, this.value );
		});

	} else {
		// If traditional, encode the "old" way (the way 1.3.2 or older
		// did it), otherwise encode params recursively.
		for ( prefix in a ) {
			buildParams( prefix, a[ prefix ], traditional, add );
		}
	}

	// Return the resulting serialization
	return s.join( "&" ).replace( r20, "+" );
};

jQuery.fn.extend({
	serialize: function() {
		return jQuery.param( this.serializeArray() );
	},
	serializeArray: function() {
		return this.map(function() {
			// Can add propHook for "elements" to filter or add form elements
			var elements = jQuery.prop( this, "elements" );
			return elements ? jQuery.makeArray( elements ) : this;
		})
		.filter(function() {
			var type = this.type;

			// Use .is( ":disabled" ) so that fieldset[disabled] works
			return this.name && !jQuery( this ).is( ":disabled" ) &&
				rsubmittable.test( this.nodeName ) && !rsubmitterTypes.test( type ) &&
				( this.checked || !rcheckableType.test( type ) );
		})
		.map(function( i, elem ) {
			var val = jQuery( this ).val();

			return val == null ?
				null :
				jQuery.isArray( val ) ?
					jQuery.map( val, function( val ) {
						return { name: elem.name, value: val.replace( rCRLF, "\r\n" ) };
					}) :
					{ name: elem.name, value: val.replace( rCRLF, "\r\n" ) };
		}).get();
	}
});


jQuery.ajaxSettings.xhr = function() {
	try {
		return new XMLHttpRequest();
	} catch( e ) {}
};

var xhrId = 0,
	xhrCallbacks = {},
	xhrSuccessStatus = {
		// file protocol always yields status code 0, assume 200
		0: 200,
		// Support: IE9
		// #1450: sometimes IE returns 1223 when it should be 204
		1223: 204
	},
	xhrSupported = jQuery.ajaxSettings.xhr();

// Support: IE9
// Open requests must be manually aborted on unload (#5280)
if ( window.ActiveXObject ) {
	jQuery( window ).on( "unload", function() {
		for ( var key in xhrCallbacks ) {
			xhrCallbacks[ key ]();
		}
	});
}

support.cors = !!xhrSupported && ( "withCredentials" in xhrSupported );
support.ajax = xhrSupported = !!xhrSupported;

jQuery.ajaxTransport(function( options ) {
	var callback;

	// Cross domain only allowed if supported through XMLHttpRequest
	if ( support.cors || xhrSupported && !options.crossDomain ) {
		return {
			send: function( headers, complete ) {
				var i,
					xhr = options.xhr(),
					id = ++xhrId;

				xhr.open( options.type, options.url, options.async, options.username, options.password );

				// Apply custom fields if provided
				if ( options.xhrFields ) {
					for ( i in options.xhrFields ) {
						xhr[ i ] = options.xhrFields[ i ];
					}
				}

				// Override mime type if needed
				if ( options.mimeType && xhr.overrideMimeType ) {
					xhr.overrideMimeType( options.mimeType );
				}

				// X-Requested-With header
				// For cross-domain requests, seeing as conditions for a preflight are
				// akin to a jigsaw puzzle, we simply never set it to be sure.
				// (it can always be set on a per-request basis or even using ajaxSetup)
				// For same-domain requests, won't change header if already provided.
				if ( !options.crossDomain && !headers["X-Requested-With"] ) {
					headers["X-Requested-With"] = "XMLHttpRequest";
				}

				// Set headers
				for ( i in headers ) {
					xhr.setRequestHeader( i, headers[ i ] );
				}

				// Callback
				callback = function( type ) {
					return function() {
						if ( callback ) {
							delete xhrCallbacks[ id ];
							callback = xhr.onload = xhr.onerror = null;

							if ( type === "abort" ) {
								xhr.abort();
							} else if ( type === "error" ) {
								complete(
									// file: protocol always yields status 0; see #8605, #14207
									xhr.status,
									xhr.statusText
								);
							} else {
								complete(
									xhrSuccessStatus[ xhr.status ] || xhr.status,
									xhr.statusText,
									// Support: IE9
									// Accessing binary-data responseText throws an exception
									// (#11426)
									typeof xhr.responseText === "string" ? {
										text: xhr.responseText
									} : undefined,
									xhr.getAllResponseHeaders()
								);
							}
						}
					};
				};

				// Listen to events
				xhr.onload = callback();
				xhr.onerror = callback("error");

				// Create the abort callback
				callback = xhrCallbacks[ id ] = callback("abort");

				try {
					// Do send the request (this may raise an exception)
					xhr.send( options.hasContent && options.data || null );
				} catch ( e ) {
					// #14683: Only rethrow if this hasn't been notified as an error yet
					if ( callback ) {
						throw e;
					}
				}
			},

			abort: function() {
				if ( callback ) {
					callback();
				}
			}
		};
	}
});




// Install script dataType
jQuery.ajaxSetup({
	accepts: {
		script: "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"
	},
	contents: {
		script: /(?:java|ecma)script/
	},
	converters: {
		"text script": function( text ) {
			jQuery.globalEval( text );
			return text;
		}
	}
});

// Handle cache's special case and crossDomain
jQuery.ajaxPrefilter( "script", function( s ) {
	if ( s.cache === undefined ) {
		s.cache = false;
	}
	if ( s.crossDomain ) {
		s.type = "GET";
	}
});

// Bind script tag hack transport
jQuery.ajaxTransport( "script", function( s ) {
	// This transport only deals with cross domain requests
	if ( s.crossDomain ) {
		var script, callback;
		return {
			send: function( _, complete ) {
				script = jQuery("<script>").prop({
					async: true,
					charset: s.scriptCharset,
					src: s.url
				}).on(
					"load error",
					callback = function( evt ) {
						script.remove();
						callback = null;
						if ( evt ) {
							complete( evt.type === "error" ? 404 : 200, evt.type );
						}
					}
				);
				document.head.appendChild( script[ 0 ] );
			},
			abort: function() {
				if ( callback ) {
					callback();
				}
			}
		};
	}
});




var oldCallbacks = [],
	rjsonp = /(=)\?(?=&|$)|\?\?/;

// Default jsonp settings
jQuery.ajaxSetup({
	jsonp: "callback",
	jsonpCallback: function() {
		var callback = oldCallbacks.pop() || ( jQuery.expando + "_" + ( nonce++ ) );
		this[ callback ] = true;
		return callback;
	}
});

// Detect, normalize options and install callbacks for jsonp requests
jQuery.ajaxPrefilter( "json jsonp", function( s, originalSettings, jqXHR ) {

	var callbackName, overwritten, responseContainer,
		jsonProp = s.jsonp !== false && ( rjsonp.test( s.url ) ?
			"url" :
			typeof s.data === "string" && !( s.contentType || "" ).indexOf("application/x-www-form-urlencoded") && rjsonp.test( s.data ) && "data"
		);

	// Handle iff the expected data type is "jsonp" or we have a parameter to set
	if ( jsonProp || s.dataTypes[ 0 ] === "jsonp" ) {

		// Get callback name, remembering preexisting value associated with it
		callbackName = s.jsonpCallback = jQuery.isFunction( s.jsonpCallback ) ?
			s.jsonpCallback() :
			s.jsonpCallback;

		// Insert callback into url or form data
		if ( jsonProp ) {
			s[ jsonProp ] = s[ jsonProp ].replace( rjsonp, "$1" + callbackName );
		} else if ( s.jsonp !== false ) {
			s.url += ( rquery.test( s.url ) ? "&" : "?" ) + s.jsonp + "=" + callbackName;
		}

		// Use data converter to retrieve json after script execution
		s.converters["script json"] = function() {
			if ( !responseContainer ) {
				jQuery.error( callbackName + " was not called" );
			}
			return responseContainer[ 0 ];
		};

		// force json dataType
		s.dataTypes[ 0 ] = "json";

		// Install callback
		overwritten = window[ callbackName ];
		window[ callbackName ] = function() {
			responseContainer = arguments;
		};

		// Clean-up function (fires after converters)
		jqXHR.always(function() {
			// Restore preexisting value
			window[ callbackName ] = overwritten;

			// Save back as free
			if ( s[ callbackName ] ) {
				// make sure that re-using the options doesn't screw things around
				s.jsonpCallback = originalSettings.jsonpCallback;

				// save the callback name for future use
				oldCallbacks.push( callbackName );
			}

			// Call if it was a function and we have a response
			if ( responseContainer && jQuery.isFunction( overwritten ) ) {
				overwritten( responseContainer[ 0 ] );
			}

			responseContainer = overwritten = undefined;
		});

		// Delegate to script
		return "script";
	}
});




// data: string of html
// context (optional): If specified, the fragment will be created in this context, defaults to document
// keepScripts (optional): If true, will include scripts passed in the html string
jQuery.parseHTML = function( data, context, keepScripts ) {
	if ( !data || typeof data !== "string" ) {
		return null;
	}
	if ( typeof context === "boolean" ) {
		keepScripts = context;
		context = false;
	}
	context = context || document;

	var parsed = rsingleTag.exec( data ),
		scripts = !keepScripts && [];

	// Single tag
	if ( parsed ) {
		return [ context.createElement( parsed[1] ) ];
	}

	parsed = jQuery.buildFragment( [ data ], context, scripts );

	if ( scripts && scripts.length ) {
		jQuery( scripts ).remove();
	}

	return jQuery.merge( [], parsed.childNodes );
};


// Keep a copy of the old load method
var _load = jQuery.fn.load;

/**
 * Load a url into a page
 */
jQuery.fn.load = function( url, params, callback ) {
	if ( typeof url !== "string" && _load ) {
		return _load.apply( this, arguments );
	}

	var selector, type, response,
		self = this,
		off = url.indexOf(" ");

	if ( off >= 0 ) {
		selector = jQuery.trim( url.slice( off ) );
		url = url.slice( 0, off );
	}

	// If it's a function
	if ( jQuery.isFunction( params ) ) {

		// We assume that it's the callback
		callback = params;
		params = undefined;

	// Otherwise, build a param string
	} else if ( params && typeof params === "object" ) {
		type = "POST";
	}

	// If we have elements to modify, make the request
	if ( self.length > 0 ) {
		jQuery.ajax({
			url: url,

			// if "type" variable is undefined, then "GET" method will be used
			type: type,
			dataType: "html",
			data: params
		}).done(function( responseText ) {

			// Save response for use in complete callback
			response = arguments;

			self.html( selector ?

				// If a selector was specified, locate the right elements in a dummy div
				// Exclude scripts to avoid IE 'Permission Denied' errors
				jQuery("<div>").append( jQuery.parseHTML( responseText ) ).find( selector ) :

				// Otherwise use the full result
				responseText );

		}).complete( callback && function( jqXHR, status ) {
			self.each( callback, response || [ jqXHR.responseText, status, jqXHR ] );
		});
	}

	return this;
};




jQuery.expr.filters.animated = function( elem ) {
	return jQuery.grep(jQuery.timers, function( fn ) {
		return elem === fn.elem;
	}).length;
};




var docElem = window.document.documentElement;

/**
 * Gets a window from an element
 */
function getWindow( elem ) {
	return jQuery.isWindow( elem ) ? elem : elem.nodeType === 9 && elem.defaultView;
}

jQuery.offset = {
	setOffset: function( elem, options, i ) {
		var curPosition, curLeft, curCSSTop, curTop, curOffset, curCSSLeft, calculatePosition,
			position = jQuery.css( elem, "position" ),
			curElem = jQuery( elem ),
			props = {};

		// Set position first, in-case top/left are set even on static elem
		if ( position === "static" ) {
			elem.style.position = "relative";
		}

		curOffset = curElem.offset();
		curCSSTop = jQuery.css( elem, "top" );
		curCSSLeft = jQuery.css( elem, "left" );
		calculatePosition = ( position === "absolute" || position === "fixed" ) &&
			( curCSSTop + curCSSLeft ).indexOf("auto") > -1;

		// Need to be able to calculate position if either top or left is auto and position is either absolute or fixed
		if ( calculatePosition ) {
			curPosition = curElem.position();
			curTop = curPosition.top;
			curLeft = curPosition.left;

		} else {
			curTop = parseFloat( curCSSTop ) || 0;
			curLeft = parseFloat( curCSSLeft ) || 0;
		}

		if ( jQuery.isFunction( options ) ) {
			options = options.call( elem, i, curOffset );
		}

		if ( options.top != null ) {
			props.top = ( options.top - curOffset.top ) + curTop;
		}
		if ( options.left != null ) {
			props.left = ( options.left - curOffset.left ) + curLeft;
		}

		if ( "using" in options ) {
			options.using.call( elem, props );

		} else {
			curElem.css( props );
		}
	}
};

jQuery.fn.extend({
	offset: function( options ) {
		if ( arguments.length ) {
			return options === undefined ?
				this :
				this.each(function( i ) {
					jQuery.offset.setOffset( this, options, i );
				});
		}

		var docElem, win,
			elem = this[ 0 ],
			box = { top: 0, left: 0 },
			doc = elem && elem.ownerDocument;

		if ( !doc ) {
			return;
		}

		docElem = doc.documentElement;

		// Make sure it's not a disconnected DOM node
		if ( !jQuery.contains( docElem, elem ) ) {
			return box;
		}

		// If we don't have gBCR, just use 0,0 rather than error
		// BlackBerry 5, iOS 3 (original iPhone)
		if ( typeof elem.getBoundingClientRect !== strundefined ) {
			box = elem.getBoundingClientRect();
		}
		win = getWindow( doc );
		return {
			top: box.top + win.pageYOffset - docElem.clientTop,
			left: box.left + win.pageXOffset - docElem.clientLeft
		};
	},

	position: function() {
		if ( !this[ 0 ] ) {
			return;
		}

		var offsetParent, offset,
			elem = this[ 0 ],
			parentOffset = { top: 0, left: 0 };

		// Fixed elements are offset from window (parentOffset = {top:0, left: 0}, because it is its only offset parent
		if ( jQuery.css( elem, "position" ) === "fixed" ) {
			// We assume that getBoundingClientRect is available when computed position is fixed
			offset = elem.getBoundingClientRect();

		} else {
			// Get *real* offsetParent
			offsetParent = this.offsetParent();

			// Get correct offsets
			offset = this.offset();
			if ( !jQuery.nodeName( offsetParent[ 0 ], "html" ) ) {
				parentOffset = offsetParent.offset();
			}

			// Add offsetParent borders
			parentOffset.top += jQuery.css( offsetParent[ 0 ], "borderTopWidth", true );
			parentOffset.left += jQuery.css( offsetParent[ 0 ], "borderLeftWidth", true );
		}

		// Subtract parent offsets and element margins
		return {
			top: offset.top - parentOffset.top - jQuery.css( elem, "marginTop", true ),
			left: offset.left - parentOffset.left - jQuery.css( elem, "marginLeft", true )
		};
	},

	offsetParent: function() {
		return this.map(function() {
			var offsetParent = this.offsetParent || docElem;

			while ( offsetParent && ( !jQuery.nodeName( offsetParent, "html" ) && jQuery.css( offsetParent, "position" ) === "static" ) ) {
				offsetParent = offsetParent.offsetParent;
			}

			return offsetParent || docElem;
		});
	}
});

// Create scrollLeft and scrollTop methods
jQuery.each( { scrollLeft: "pageXOffset", scrollTop: "pageYOffset" }, function( method, prop ) {
	var top = "pageYOffset" === prop;

	jQuery.fn[ method ] = function( val ) {
		return access( this, function( elem, method, val ) {
			var win = getWindow( elem );

			if ( val === undefined ) {
				return win ? win[ prop ] : elem[ method ];
			}

			if ( win ) {
				win.scrollTo(
					!top ? val : window.pageXOffset,
					top ? val : window.pageYOffset
				);

			} else {
				elem[ method ] = val;
			}
		}, method, val, arguments.length, null );
	};
});

// Add the top/left cssHooks using jQuery.fn.position
// Webkit bug: https://bugs.webkit.org/show_bug.cgi?id=29084
// getComputedStyle returns percent when specified for top/left/bottom/right
// rather than make the css module depend on the offset module, we just check for it here
jQuery.each( [ "top", "left" ], function( i, prop ) {
	jQuery.cssHooks[ prop ] = addGetHookIf( support.pixelPosition,
		function( elem, computed ) {
			if ( computed ) {
				computed = curCSS( elem, prop );
				// if curCSS returns percentage, fallback to offset
				return rnumnonpx.test( computed ) ?
					jQuery( elem ).position()[ prop ] + "px" :
					computed;
			}
		}
	);
});


// Create innerHeight, innerWidth, height, width, outerHeight and outerWidth methods
jQuery.each( { Height: "height", Width: "width" }, function( name, type ) {
	jQuery.each( { padding: "inner" + name, content: type, "": "outer" + name }, function( defaultExtra, funcName ) {
		// margin is only for outerHeight, outerWidth
		jQuery.fn[ funcName ] = function( margin, value ) {
			var chainable = arguments.length && ( defaultExtra || typeof margin !== "boolean" ),
				extra = defaultExtra || ( margin === true || value === true ? "margin" : "border" );

			return access( this, function( elem, type, value ) {
				var doc;

				if ( jQuery.isWindow( elem ) ) {
					// As of 5/8/2012 this will yield incorrect results for Mobile Safari, but there
					// isn't a whole lot we can do. See pull request at this URL for discussion:
					// https://github.com/jquery/jquery/pull/764
					return elem.document.documentElement[ "client" + name ];
				}

				// Get document width or height
				if ( elem.nodeType === 9 ) {
					doc = elem.documentElement;

					// Either scroll[Width/Height] or offset[Width/Height] or client[Width/Height],
					// whichever is greatest
					return Math.max(
						elem.body[ "scroll" + name ], doc[ "scroll" + name ],
						elem.body[ "offset" + name ], doc[ "offset" + name ],
						doc[ "client" + name ]
					);
				}

				return value === undefined ?
					// Get width or height on the element, requesting but not forcing parseFloat
					jQuery.css( elem, type, extra ) :

					// Set width or height on the element
					jQuery.style( elem, type, value, extra );
			}, type, chainable ? margin : undefined, chainable, null );
		};
	});
});


// The number of elements contained in the matched element set
jQuery.fn.size = function() {
	return this.length;
};

jQuery.fn.andSelf = jQuery.fn.addBack;




// Register as a named AMD module, since jQuery can be concatenated with other
// files that may use define, but not via a proper concatenation script that
// understands anonymous AMD modules. A named AMD is safest and most robust
// way to register. Lowercase jquery is used because AMD module names are
// derived from file names, and jQuery is normally delivered in a lowercase
// file name. Do this after creating the global so that if an AMD module wants
// to call noConflict to hide this version of jQuery, it will work.

// Note that for maximum portability, libraries that are not jQuery should
// declare themselves as anonymous modules, and avoid setting a global if an
// AMD loader is present. jQuery is a special case. For more information, see
// https://github.com/jrburke/requirejs/wiki/Updating-existing-libraries#wiki-anon

if ( typeof define === "function" && define.amd ) {
	define( "jquery", [], function() {
		return jQuery;
	});
}




var
	// Map over jQuery in case of overwrite
	_jQuery = window.jQuery,

	// Map over the $ in case of overwrite
	_$ = window.$;

jQuery.noConflict = function( deep ) {
	if ( window.$ === jQuery ) {
		window.$ = _$;
	}

	if ( deep && window.jQuery === jQuery ) {
		window.jQuery = _jQuery;
	}

	return jQuery;
};

// Expose jQuery and $ identifiers, even in
// AMD (#7102#comment:10, https://github.com/jquery/jquery/pull/557)
// and CommonJS for browser emulators (#13566)
if ( typeof noGlobal === strundefined ) {
	window.jQuery = window.$ = jQuery;
}




return jQuery;

}));

},{}],6:[function(require,module,exports){
/*
 * Toastr
 * Copyright 2012-2014 John Papa and Hans Fjällemark.
 * All Rights Reserved.
 * Use, reproduction, distribution, and modification of this code is subject to the terms and
 * conditions of the MIT license, available at http://www.opensource.org/licenses/mit-license.php
 *
 * Author: John Papa and Hans Fjällemark
 * ARIA Support: Greta Krafsig
 * Project: https://github.com/CodeSeven/toastr
 */
; (function (define) {
    define(['jquery'], function ($) {
        return (function () {
            var $container;
            var listener;
            var toastId = 0;
            var toastType = {
                error: 'error',
                info: 'info',
                success: 'success',
                warning: 'warning'
            };

            var toastr = {
                clear: clear,
                remove: remove,
                error: error,
                getContainer: getContainer,
                info: info,
                options: {},
                subscribe: subscribe,
                success: success,
                version: '2.0.3',
                warning: warning
            };

            return toastr;

            //#region Accessible Methods
            function error(message, title, optionsOverride) {
                return notify({
                    type: toastType.error,
                    iconClass: getOptions().iconClasses.error,
                    message: message,
                    optionsOverride: optionsOverride,
                    title: title
                });
            }

            function getContainer(options, create) {
                if (!options) { options = getOptions(); }
                $container = $('#' + options.containerId);
                if ($container.length) {
                    return $container;
                }
                if(create) {
                    $container = createContainer(options);
                }
                return $container;
            }

            function info(message, title, optionsOverride) {
                return notify({
                    type: toastType.info,
                    iconClass: getOptions().iconClasses.info,
                    message: message,
                    optionsOverride: optionsOverride,
                    title: title
                });
            }

            function subscribe(callback) {
                listener = callback;
            }

            function success(message, title, optionsOverride) {
                return notify({
                    type: toastType.success,
                    iconClass: getOptions().iconClasses.success,
                    message: message,
                    optionsOverride: optionsOverride,
                    title: title
                });
            }

            function warning(message, title, optionsOverride) {
                return notify({
                    type: toastType.warning,
                    iconClass: getOptions().iconClasses.warning,
                    message: message,
                    optionsOverride: optionsOverride,
                    title: title
                });
            }

            function clear($toastElement) {
                var options = getOptions();
                if (!$container) { getContainer(options); }
                if (!clearToast($toastElement, options)) {
                    clearContainer(options);
                }
            }

            function remove($toastElement) {
                var options = getOptions();
                if (!$container) { getContainer(options); }
                if ($toastElement && $(':focus', $toastElement).length === 0) {
                    removeToast($toastElement);
                    return;
                }
                if ($container.children().length) {
                    $container.remove();
                }
            }
            //#endregion

            //#region Internal Methods

            function clearContainer(options){
                var toastsToClear = $container.children();
                for (var i = toastsToClear.length - 1; i >= 0; i--) {
                    clearToast($(toastsToClear[i]), options);
                };
            }

            function clearToast($toastElement, options){
                if ($toastElement && $(':focus', $toastElement).length === 0) {
                    $toastElement[options.hideMethod]({
                        duration: options.hideDuration,
                        easing: options.hideEasing,
                        complete: function () { removeToast($toastElement); }
                    });
                    return true;
                }
                return false;
            }

            function createContainer(options) {
                $container = $('<div/>')
                    .attr('id', options.containerId)
                    .addClass(options.positionClass)
                    .attr('aria-live', 'polite')
                    .attr('role', 'alert');

                $container.appendTo($(options.target));
                return $container;
            }

            function getDefaults() {
                return {
                    tapToDismiss: true,
                    toastClass: 'toast',
                    containerId: 'toast-container',
                    debug: false,

                    showMethod: 'fadeIn', //fadeIn, slideDown, and show are built into jQuery
                    showDuration: 300,
                    showEasing: 'swing', //swing and linear are built into jQuery
                    onShown: undefined,
                    hideMethod: 'fadeOut',
                    hideDuration: 1000,
                    hideEasing: 'swing',
                    onHidden: undefined,

                    extendedTimeOut: 1000,
                    iconClasses: {
                        error: 'toast-error',
                        info: 'toast-info',
                        success: 'toast-success',
                        warning: 'toast-warning'
                    },
                    iconClass: 'toast-info',
                    positionClass: 'toast-top-right',
                    timeOut: 5000, // Set timeOut and extendedTimeout to 0 to make it sticky
                    titleClass: 'toast-title',
                    messageClass: 'toast-message',
                    target: 'body',
                    closeHtml: '<button>&times;</button>',
                    newestOnTop: true
                };
            }

            function publish(args) {
                if (!listener) { return; }
                listener(args);
            }

            function notify(map) {
                var options = getOptions(),
                    iconClass = map.iconClass || options.iconClass;

                if (typeof (map.optionsOverride) !== 'undefined') {
                    options = $.extend(options, map.optionsOverride);
                    iconClass = map.optionsOverride.iconClass || iconClass;
                }

                toastId++;

                $container = getContainer(options, true);
                var intervalId = null,
                    $toastElement = $('<div/>'),
                    $titleElement = $('<div/>'),
                    $messageElement = $('<div/>'),
                    $closeElement = $(options.closeHtml),
                    response = {
                        toastId: toastId,
                        state: 'visible',
                        startTime: new Date(),
                        options: options,
                        map: map
                    };

                if (map.iconClass) {
                    $toastElement.addClass(options.toastClass).addClass(iconClass);
                }

                if (map.title) {
                    $titleElement.append(map.title).addClass(options.titleClass);
                    $toastElement.append($titleElement);
                }

                if (map.message) {
                    $messageElement.append(map.message).addClass(options.messageClass);
                    $toastElement.append($messageElement);
                }

                if (options.closeButton) {
                    $closeElement.addClass('toast-close-button').attr("role", "button");
                    $toastElement.prepend($closeElement);
                }

                $toastElement.hide();
                if (options.newestOnTop) {
                    $container.prepend($toastElement);
                } else {
                    $container.append($toastElement);
                }


                $toastElement[options.showMethod](
                    { duration: options.showDuration, easing: options.showEasing, complete: options.onShown }
                );

                if (options.timeOut > 0) {
                    intervalId = setTimeout(hideToast, options.timeOut);
                }

                $toastElement.hover(stickAround, delayedHideToast);
                if (!options.onclick && options.tapToDismiss) {
                    $toastElement.click(hideToast);
                }

                if (options.closeButton && $closeElement) {
                    $closeElement.click(function (event) {
                        if( event.stopPropagation ) {
                            event.stopPropagation();
                        } else if( event.cancelBubble !== undefined && event.cancelBubble !== true ) {
                            event.cancelBubble = true;
                        }
                        hideToast(true);
                    });
                }

                if (options.onclick) {
                    $toastElement.click(function () {
                        options.onclick();
                        hideToast();
                    });
                }

                publish(response);

                if (options.debug && console) {
                    console.log(response);
                }

                return $toastElement;

                function hideToast(override) {
                    if ($(':focus', $toastElement).length && !override) {
                        return;
                    }
                    return $toastElement[options.hideMethod]({
                        duration: options.hideDuration,
                        easing: options.hideEasing,
                        complete: function () {
                            removeToast($toastElement);
                            if (options.onHidden && response.state !== 'hidden') {
                                options.onHidden();
                            }
                            response.state = 'hidden';
                            response.endTime = new Date();
                            publish(response);
                        }
                    });
                }

                function delayedHideToast() {
                    if (options.timeOut > 0 || options.extendedTimeOut > 0) {
                        intervalId = setTimeout(hideToast, options.extendedTimeOut);
                    }
                }

                function stickAround() {
                    clearTimeout(intervalId);
                    $toastElement.stop(true, true)[options.showMethod](
                        { duration: options.showDuration, easing: options.showEasing }
                    );
                }
            }

            function getOptions() {
                return $.extend({}, getDefaults(), toastr.options);
            }

            function removeToast($toastElement) {
                if (!$container) { $container = getContainer(); }
                if ($toastElement.is(':visible')) {
                    return;
                }
                $toastElement.remove();
                $toastElement = null;
                if ($container.children().length === 0) {
                    $container.remove();
                }
            }
            //#endregion

        })();
    });
}(typeof define === 'function' && define.amd ? define : function (deps, factory) {
    if (typeof module !== 'undefined' && module.exports) { //Node
        module.exports = factory(require('jquery'));
    } else {
        window['toastr'] = factory(window['jQuery']);
    }
}));
},{"jquery":5}],7:[function(require,module,exports){
//     Underscore.js 1.7.0
//     http://underscorejs.org
//     (c) 2009-2014 Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
//     Underscore may be freely distributed under the MIT license.

(function() {

  // Baseline setup
  // --------------

  // Establish the root object, `window` in the browser, or `exports` on the server.
  var root = this;

  // Save the previous value of the `_` variable.
  var previousUnderscore = root._;

  // Save bytes in the minified (but not gzipped) version:
  var ArrayProto = Array.prototype, ObjProto = Object.prototype, FuncProto = Function.prototype;

  // Create quick reference variables for speed access to core prototypes.
  var
    push             = ArrayProto.push,
    slice            = ArrayProto.slice,
    concat           = ArrayProto.concat,
    toString         = ObjProto.toString,
    hasOwnProperty   = ObjProto.hasOwnProperty;

  // All **ECMAScript 5** native function implementations that we hope to use
  // are declared here.
  var
    nativeIsArray      = Array.isArray,
    nativeKeys         = Object.keys,
    nativeBind         = FuncProto.bind;

  // Create a safe reference to the Underscore object for use below.
  var _ = function(obj) {
    if (obj instanceof _) return obj;
    if (!(this instanceof _)) return new _(obj);
    this._wrapped = obj;
  };

  // Export the Underscore object for **Node.js**, with
  // backwards-compatibility for the old `require()` API. If we're in
  // the browser, add `_` as a global object.
  if (typeof exports !== 'undefined') {
    if (typeof module !== 'undefined' && module.exports) {
      exports = module.exports = _;
    }
    exports._ = _;
  } else {
    root._ = _;
  }

  // Current version.
  _.VERSION = '1.7.0';

  // Internal function that returns an efficient (for current engines) version
  // of the passed-in callback, to be repeatedly applied in other Underscore
  // functions.
  var createCallback = function(func, context, argCount) {
    if (context === void 0) return func;
    switch (argCount == null ? 3 : argCount) {
      case 1: return function(value) {
        return func.call(context, value);
      };
      case 2: return function(value, other) {
        return func.call(context, value, other);
      };
      case 3: return function(value, index, collection) {
        return func.call(context, value, index, collection);
      };
      case 4: return function(accumulator, value, index, collection) {
        return func.call(context, accumulator, value, index, collection);
      };
    }
    return function() {
      return func.apply(context, arguments);
    };
  };

  // A mostly-internal function to generate callbacks that can be applied
  // to each element in a collection, returning the desired result — either
  // identity, an arbitrary callback, a property matcher, or a property accessor.
  _.iteratee = function(value, context, argCount) {
    if (value == null) return _.identity;
    if (_.isFunction(value)) return createCallback(value, context, argCount);
    if (_.isObject(value)) return _.matches(value);
    return _.property(value);
  };

  // Collection Functions
  // --------------------

  // The cornerstone, an `each` implementation, aka `forEach`.
  // Handles raw objects in addition to array-likes. Treats all
  // sparse array-likes as if they were dense.
  _.each = _.forEach = function(obj, iteratee, context) {
    if (obj == null) return obj;
    iteratee = createCallback(iteratee, context);
    var i, length = obj.length;
    if (length === +length) {
      for (i = 0; i < length; i++) {
        iteratee(obj[i], i, obj);
      }
    } else {
      var keys = _.keys(obj);
      for (i = 0, length = keys.length; i < length; i++) {
        iteratee(obj[keys[i]], keys[i], obj);
      }
    }
    return obj;
  };

  // Return the results of applying the iteratee to each element.
  _.map = _.collect = function(obj, iteratee, context) {
    if (obj == null) return [];
    iteratee = _.iteratee(iteratee, context);
    var keys = obj.length !== +obj.length && _.keys(obj),
        length = (keys || obj).length,
        results = Array(length),
        currentKey;
    for (var index = 0; index < length; index++) {
      currentKey = keys ? keys[index] : index;
      results[index] = iteratee(obj[currentKey], currentKey, obj);
    }
    return results;
  };

  var reduceError = 'Reduce of empty array with no initial value';

  // **Reduce** builds up a single result from a list of values, aka `inject`,
  // or `foldl`.
  _.reduce = _.foldl = _.inject = function(obj, iteratee, memo, context) {
    if (obj == null) obj = [];
    iteratee = createCallback(iteratee, context, 4);
    var keys = obj.length !== +obj.length && _.keys(obj),
        length = (keys || obj).length,
        index = 0, currentKey;
    if (arguments.length < 3) {
      if (!length) throw new TypeError(reduceError);
      memo = obj[keys ? keys[index++] : index++];
    }
    for (; index < length; index++) {
      currentKey = keys ? keys[index] : index;
      memo = iteratee(memo, obj[currentKey], currentKey, obj);
    }
    return memo;
  };

  // The right-associative version of reduce, also known as `foldr`.
  _.reduceRight = _.foldr = function(obj, iteratee, memo, context) {
    if (obj == null) obj = [];
    iteratee = createCallback(iteratee, context, 4);
    var keys = obj.length !== + obj.length && _.keys(obj),
        index = (keys || obj).length,
        currentKey;
    if (arguments.length < 3) {
      if (!index) throw new TypeError(reduceError);
      memo = obj[keys ? keys[--index] : --index];
    }
    while (index--) {
      currentKey = keys ? keys[index] : index;
      memo = iteratee(memo, obj[currentKey], currentKey, obj);
    }
    return memo;
  };

  // Return the first value which passes a truth test. Aliased as `detect`.
  _.find = _.detect = function(obj, predicate, context) {
    var result;
    predicate = _.iteratee(predicate, context);
    _.some(obj, function(value, index, list) {
      if (predicate(value, index, list)) {
        result = value;
        return true;
      }
    });
    return result;
  };

  // Return all the elements that pass a truth test.
  // Aliased as `select`.
  _.filter = _.select = function(obj, predicate, context) {
    var results = [];
    if (obj == null) return results;
    predicate = _.iteratee(predicate, context);
    _.each(obj, function(value, index, list) {
      if (predicate(value, index, list)) results.push(value);
    });
    return results;
  };

  // Return all the elements for which a truth test fails.
  _.reject = function(obj, predicate, context) {
    return _.filter(obj, _.negate(_.iteratee(predicate)), context);
  };

  // Determine whether all of the elements match a truth test.
  // Aliased as `all`.
  _.every = _.all = function(obj, predicate, context) {
    if (obj == null) return true;
    predicate = _.iteratee(predicate, context);
    var keys = obj.length !== +obj.length && _.keys(obj),
        length = (keys || obj).length,
        index, currentKey;
    for (index = 0; index < length; index++) {
      currentKey = keys ? keys[index] : index;
      if (!predicate(obj[currentKey], currentKey, obj)) return false;
    }
    return true;
  };

  // Determine if at least one element in the object matches a truth test.
  // Aliased as `any`.
  _.some = _.any = function(obj, predicate, context) {
    if (obj == null) return false;
    predicate = _.iteratee(predicate, context);
    var keys = obj.length !== +obj.length && _.keys(obj),
        length = (keys || obj).length,
        index, currentKey;
    for (index = 0; index < length; index++) {
      currentKey = keys ? keys[index] : index;
      if (predicate(obj[currentKey], currentKey, obj)) return true;
    }
    return false;
  };

  // Determine if the array or object contains a given value (using `===`).
  // Aliased as `include`.
  _.contains = _.include = function(obj, target) {
    if (obj == null) return false;
    if (obj.length !== +obj.length) obj = _.values(obj);
    return _.indexOf(obj, target) >= 0;
  };

  // Invoke a method (with arguments) on every item in a collection.
  _.invoke = function(obj, method) {
    var args = slice.call(arguments, 2);
    var isFunc = _.isFunction(method);
    return _.map(obj, function(value) {
      return (isFunc ? method : value[method]).apply(value, args);
    });
  };

  // Convenience version of a common use case of `map`: fetching a property.
  _.pluck = function(obj, key) {
    return _.map(obj, _.property(key));
  };

  // Convenience version of a common use case of `filter`: selecting only objects
  // containing specific `key:value` pairs.
  _.where = function(obj, attrs) {
    return _.filter(obj, _.matches(attrs));
  };

  // Convenience version of a common use case of `find`: getting the first object
  // containing specific `key:value` pairs.
  _.findWhere = function(obj, attrs) {
    return _.find(obj, _.matches(attrs));
  };

  // Return the maximum element (or element-based computation).
  _.max = function(obj, iteratee, context) {
    var result = -Infinity, lastComputed = -Infinity,
        value, computed;
    if (iteratee == null && obj != null) {
      obj = obj.length === +obj.length ? obj : _.values(obj);
      for (var i = 0, length = obj.length; i < length; i++) {
        value = obj[i];
        if (value > result) {
          result = value;
        }
      }
    } else {
      iteratee = _.iteratee(iteratee, context);
      _.each(obj, function(value, index, list) {
        computed = iteratee(value, index, list);
        if (computed > lastComputed || computed === -Infinity && result === -Infinity) {
          result = value;
          lastComputed = computed;
        }
      });
    }
    return result;
  };

  // Return the minimum element (or element-based computation).
  _.min = function(obj, iteratee, context) {
    var result = Infinity, lastComputed = Infinity,
        value, computed;
    if (iteratee == null && obj != null) {
      obj = obj.length === +obj.length ? obj : _.values(obj);
      for (var i = 0, length = obj.length; i < length; i++) {
        value = obj[i];
        if (value < result) {
          result = value;
        }
      }
    } else {
      iteratee = _.iteratee(iteratee, context);
      _.each(obj, function(value, index, list) {
        computed = iteratee(value, index, list);
        if (computed < lastComputed || computed === Infinity && result === Infinity) {
          result = value;
          lastComputed = computed;
        }
      });
    }
    return result;
  };

  // Shuffle a collection, using the modern version of the
  // [Fisher-Yates shuffle](http://en.wikipedia.org/wiki/Fisher–Yates_shuffle).
  _.shuffle = function(obj) {
    var set = obj && obj.length === +obj.length ? obj : _.values(obj);
    var length = set.length;
    var shuffled = Array(length);
    for (var index = 0, rand; index < length; index++) {
      rand = _.random(0, index);
      if (rand !== index) shuffled[index] = shuffled[rand];
      shuffled[rand] = set[index];
    }
    return shuffled;
  };

  // Sample **n** random values from a collection.
  // If **n** is not specified, returns a single random element.
  // The internal `guard` argument allows it to work with `map`.
  _.sample = function(obj, n, guard) {
    if (n == null || guard) {
      if (obj.length !== +obj.length) obj = _.values(obj);
      return obj[_.random(obj.length - 1)];
    }
    return _.shuffle(obj).slice(0, Math.max(0, n));
  };

  // Sort the object's values by a criterion produced by an iteratee.
  _.sortBy = function(obj, iteratee, context) {
    iteratee = _.iteratee(iteratee, context);
    return _.pluck(_.map(obj, function(value, index, list) {
      return {
        value: value,
        index: index,
        criteria: iteratee(value, index, list)
      };
    }).sort(function(left, right) {
      var a = left.criteria;
      var b = right.criteria;
      if (a !== b) {
        if (a > b || a === void 0) return 1;
        if (a < b || b === void 0) return -1;
      }
      return left.index - right.index;
    }), 'value');
  };

  // An internal function used for aggregate "group by" operations.
  var group = function(behavior) {
    return function(obj, iteratee, context) {
      var result = {};
      iteratee = _.iteratee(iteratee, context);
      _.each(obj, function(value, index) {
        var key = iteratee(value, index, obj);
        behavior(result, value, key);
      });
      return result;
    };
  };

  // Groups the object's values by a criterion. Pass either a string attribute
  // to group by, or a function that returns the criterion.
  _.groupBy = group(function(result, value, key) {
    if (_.has(result, key)) result[key].push(value); else result[key] = [value];
  });

  // Indexes the object's values by a criterion, similar to `groupBy`, but for
  // when you know that your index values will be unique.
  _.indexBy = group(function(result, value, key) {
    result[key] = value;
  });

  // Counts instances of an object that group by a certain criterion. Pass
  // either a string attribute to count by, or a function that returns the
  // criterion.
  _.countBy = group(function(result, value, key) {
    if (_.has(result, key)) result[key]++; else result[key] = 1;
  });

  // Use a comparator function to figure out the smallest index at which
  // an object should be inserted so as to maintain order. Uses binary search.
  _.sortedIndex = function(array, obj, iteratee, context) {
    iteratee = _.iteratee(iteratee, context, 1);
    var value = iteratee(obj);
    var low = 0, high = array.length;
    while (low < high) {
      var mid = low + high >>> 1;
      if (iteratee(array[mid]) < value) low = mid + 1; else high = mid;
    }
    return low;
  };

  // Safely create a real, live array from anything iterable.
  _.toArray = function(obj) {
    if (!obj) return [];
    if (_.isArray(obj)) return slice.call(obj);
    if (obj.length === +obj.length) return _.map(obj, _.identity);
    return _.values(obj);
  };

  // Return the number of elements in an object.
  _.size = function(obj) {
    if (obj == null) return 0;
    return obj.length === +obj.length ? obj.length : _.keys(obj).length;
  };

  // Split a collection into two arrays: one whose elements all satisfy the given
  // predicate, and one whose elements all do not satisfy the predicate.
  _.partition = function(obj, predicate, context) {
    predicate = _.iteratee(predicate, context);
    var pass = [], fail = [];
    _.each(obj, function(value, key, obj) {
      (predicate(value, key, obj) ? pass : fail).push(value);
    });
    return [pass, fail];
  };

  // Array Functions
  // ---------------

  // Get the first element of an array. Passing **n** will return the first N
  // values in the array. Aliased as `head` and `take`. The **guard** check
  // allows it to work with `_.map`.
  _.first = _.head = _.take = function(array, n, guard) {
    if (array == null) return void 0;
    if (n == null || guard) return array[0];
    if (n < 0) return [];
    return slice.call(array, 0, n);
  };

  // Returns everything but the last entry of the array. Especially useful on
  // the arguments object. Passing **n** will return all the values in
  // the array, excluding the last N. The **guard** check allows it to work with
  // `_.map`.
  _.initial = function(array, n, guard) {
    return slice.call(array, 0, Math.max(0, array.length - (n == null || guard ? 1 : n)));
  };

  // Get the last element of an array. Passing **n** will return the last N
  // values in the array. The **guard** check allows it to work with `_.map`.
  _.last = function(array, n, guard) {
    if (array == null) return void 0;
    if (n == null || guard) return array[array.length - 1];
    return slice.call(array, Math.max(array.length - n, 0));
  };

  // Returns everything but the first entry of the array. Aliased as `tail` and `drop`.
  // Especially useful on the arguments object. Passing an **n** will return
  // the rest N values in the array. The **guard**
  // check allows it to work with `_.map`.
  _.rest = _.tail = _.drop = function(array, n, guard) {
    return slice.call(array, n == null || guard ? 1 : n);
  };

  // Trim out all falsy values from an array.
  _.compact = function(array) {
    return _.filter(array, _.identity);
  };

  // Internal implementation of a recursive `flatten` function.
  var flatten = function(input, shallow, strict, output) {
    if (shallow && _.every(input, _.isArray)) {
      return concat.apply(output, input);
    }
    for (var i = 0, length = input.length; i < length; i++) {
      var value = input[i];
      if (!_.isArray(value) && !_.isArguments(value)) {
        if (!strict) output.push(value);
      } else if (shallow) {
        push.apply(output, value);
      } else {
        flatten(value, shallow, strict, output);
      }
    }
    return output;
  };

  // Flatten out an array, either recursively (by default), or just one level.
  _.flatten = function(array, shallow) {
    return flatten(array, shallow, false, []);
  };

  // Return a version of the array that does not contain the specified value(s).
  _.without = function(array) {
    return _.difference(array, slice.call(arguments, 1));
  };

  // Produce a duplicate-free version of the array. If the array has already
  // been sorted, you have the option of using a faster algorithm.
  // Aliased as `unique`.
  _.uniq = _.unique = function(array, isSorted, iteratee, context) {
    if (array == null) return [];
    if (!_.isBoolean(isSorted)) {
      context = iteratee;
      iteratee = isSorted;
      isSorted = false;
    }
    if (iteratee != null) iteratee = _.iteratee(iteratee, context);
    var result = [];
    var seen = [];
    for (var i = 0, length = array.length; i < length; i++) {
      var value = array[i];
      if (isSorted) {
        if (!i || seen !== value) result.push(value);
        seen = value;
      } else if (iteratee) {
        var computed = iteratee(value, i, array);
        if (_.indexOf(seen, computed) < 0) {
          seen.push(computed);
          result.push(value);
        }
      } else if (_.indexOf(result, value) < 0) {
        result.push(value);
      }
    }
    return result;
  };

  // Produce an array that contains the union: each distinct element from all of
  // the passed-in arrays.
  _.union = function() {
    return _.uniq(flatten(arguments, true, true, []));
  };

  // Produce an array that contains every item shared between all the
  // passed-in arrays.
  _.intersection = function(array) {
    if (array == null) return [];
    var result = [];
    var argsLength = arguments.length;
    for (var i = 0, length = array.length; i < length; i++) {
      var item = array[i];
      if (_.contains(result, item)) continue;
      for (var j = 1; j < argsLength; j++) {
        if (!_.contains(arguments[j], item)) break;
      }
      if (j === argsLength) result.push(item);
    }
    return result;
  };

  // Take the difference between one array and a number of other arrays.
  // Only the elements present in just the first array will remain.
  _.difference = function(array) {
    var rest = flatten(slice.call(arguments, 1), true, true, []);
    return _.filter(array, function(value){
      return !_.contains(rest, value);
    });
  };

  // Zip together multiple lists into a single array -- elements that share
  // an index go together.
  _.zip = function(array) {
    if (array == null) return [];
    var length = _.max(arguments, 'length').length;
    var results = Array(length);
    for (var i = 0; i < length; i++) {
      results[i] = _.pluck(arguments, i);
    }
    return results;
  };

  // Converts lists into objects. Pass either a single array of `[key, value]`
  // pairs, or two parallel arrays of the same length -- one of keys, and one of
  // the corresponding values.
  _.object = function(list, values) {
    if (list == null) return {};
    var result = {};
    for (var i = 0, length = list.length; i < length; i++) {
      if (values) {
        result[list[i]] = values[i];
      } else {
        result[list[i][0]] = list[i][1];
      }
    }
    return result;
  };

  // Return the position of the first occurrence of an item in an array,
  // or -1 if the item is not included in the array.
  // If the array is large and already in sort order, pass `true`
  // for **isSorted** to use binary search.
  _.indexOf = function(array, item, isSorted) {
    if (array == null) return -1;
    var i = 0, length = array.length;
    if (isSorted) {
      if (typeof isSorted == 'number') {
        i = isSorted < 0 ? Math.max(0, length + isSorted) : isSorted;
      } else {
        i = _.sortedIndex(array, item);
        return array[i] === item ? i : -1;
      }
    }
    for (; i < length; i++) if (array[i] === item) return i;
    return -1;
  };

  _.lastIndexOf = function(array, item, from) {
    if (array == null) return -1;
    var idx = array.length;
    if (typeof from == 'number') {
      idx = from < 0 ? idx + from + 1 : Math.min(idx, from + 1);
    }
    while (--idx >= 0) if (array[idx] === item) return idx;
    return -1;
  };

  // Generate an integer Array containing an arithmetic progression. A port of
  // the native Python `range()` function. See
  // [the Python documentation](http://docs.python.org/library/functions.html#range).
  _.range = function(start, stop, step) {
    if (arguments.length <= 1) {
      stop = start || 0;
      start = 0;
    }
    step = step || 1;

    var length = Math.max(Math.ceil((stop - start) / step), 0);
    var range = Array(length);

    for (var idx = 0; idx < length; idx++, start += step) {
      range[idx] = start;
    }

    return range;
  };

  // Function (ahem) Functions
  // ------------------

  // Reusable constructor function for prototype setting.
  var Ctor = function(){};

  // Create a function bound to a given object (assigning `this`, and arguments,
  // optionally). Delegates to **ECMAScript 5**'s native `Function.bind` if
  // available.
  _.bind = function(func, context) {
    var args, bound;
    if (nativeBind && func.bind === nativeBind) return nativeBind.apply(func, slice.call(arguments, 1));
    if (!_.isFunction(func)) throw new TypeError('Bind must be called on a function');
    args = slice.call(arguments, 2);
    bound = function() {
      if (!(this instanceof bound)) return func.apply(context, args.concat(slice.call(arguments)));
      Ctor.prototype = func.prototype;
      var self = new Ctor;
      Ctor.prototype = null;
      var result = func.apply(self, args.concat(slice.call(arguments)));
      if (_.isObject(result)) return result;
      return self;
    };
    return bound;
  };

  // Partially apply a function by creating a version that has had some of its
  // arguments pre-filled, without changing its dynamic `this` context. _ acts
  // as a placeholder, allowing any combination of arguments to be pre-filled.
  _.partial = function(func) {
    var boundArgs = slice.call(arguments, 1);
    return function() {
      var position = 0;
      var args = boundArgs.slice();
      for (var i = 0, length = args.length; i < length; i++) {
        if (args[i] === _) args[i] = arguments[position++];
      }
      while (position < arguments.length) args.push(arguments[position++]);
      return func.apply(this, args);
    };
  };

  // Bind a number of an object's methods to that object. Remaining arguments
  // are the method names to be bound. Useful for ensuring that all callbacks
  // defined on an object belong to it.
  _.bindAll = function(obj) {
    var i, length = arguments.length, key;
    if (length <= 1) throw new Error('bindAll must be passed function names');
    for (i = 1; i < length; i++) {
      key = arguments[i];
      obj[key] = _.bind(obj[key], obj);
    }
    return obj;
  };

  // Memoize an expensive function by storing its results.
  _.memoize = function(func, hasher) {
    var memoize = function(key) {
      var cache = memoize.cache;
      var address = hasher ? hasher.apply(this, arguments) : key;
      if (!_.has(cache, address)) cache[address] = func.apply(this, arguments);
      return cache[address];
    };
    memoize.cache = {};
    return memoize;
  };

  // Delays a function for the given number of milliseconds, and then calls
  // it with the arguments supplied.
  _.delay = function(func, wait) {
    var args = slice.call(arguments, 2);
    return setTimeout(function(){
      return func.apply(null, args);
    }, wait);
  };

  // Defers a function, scheduling it to run after the current call stack has
  // cleared.
  _.defer = function(func) {
    return _.delay.apply(_, [func, 1].concat(slice.call(arguments, 1)));
  };

  // Returns a function, that, when invoked, will only be triggered at most once
  // during a given window of time. Normally, the throttled function will run
  // as much as it can, without ever going more than once per `wait` duration;
  // but if you'd like to disable the execution on the leading edge, pass
  // `{leading: false}`. To disable execution on the trailing edge, ditto.
  _.throttle = function(func, wait, options) {
    var context, args, result;
    var timeout = null;
    var previous = 0;
    if (!options) options = {};
    var later = function() {
      previous = options.leading === false ? 0 : _.now();
      timeout = null;
      result = func.apply(context, args);
      if (!timeout) context = args = null;
    };
    return function() {
      var now = _.now();
      if (!previous && options.leading === false) previous = now;
      var remaining = wait - (now - previous);
      context = this;
      args = arguments;
      if (remaining <= 0 || remaining > wait) {
        clearTimeout(timeout);
        timeout = null;
        previous = now;
        result = func.apply(context, args);
        if (!timeout) context = args = null;
      } else if (!timeout && options.trailing !== false) {
        timeout = setTimeout(later, remaining);
      }
      return result;
    };
  };

  // Returns a function, that, as long as it continues to be invoked, will not
  // be triggered. The function will be called after it stops being called for
  // N milliseconds. If `immediate` is passed, trigger the function on the
  // leading edge, instead of the trailing.
  _.debounce = function(func, wait, immediate) {
    var timeout, args, context, timestamp, result;

    var later = function() {
      var last = _.now() - timestamp;

      if (last < wait && last > 0) {
        timeout = setTimeout(later, wait - last);
      } else {
        timeout = null;
        if (!immediate) {
          result = func.apply(context, args);
          if (!timeout) context = args = null;
        }
      }
    };

    return function() {
      context = this;
      args = arguments;
      timestamp = _.now();
      var callNow = immediate && !timeout;
      if (!timeout) timeout = setTimeout(later, wait);
      if (callNow) {
        result = func.apply(context, args);
        context = args = null;
      }

      return result;
    };
  };

  // Returns the first function passed as an argument to the second,
  // allowing you to adjust arguments, run code before and after, and
  // conditionally execute the original function.
  _.wrap = function(func, wrapper) {
    return _.partial(wrapper, func);
  };

  // Returns a negated version of the passed-in predicate.
  _.negate = function(predicate) {
    return function() {
      return !predicate.apply(this, arguments);
    };
  };

  // Returns a function that is the composition of a list of functions, each
  // consuming the return value of the function that follows.
  _.compose = function() {
    var args = arguments;
    var start = args.length - 1;
    return function() {
      var i = start;
      var result = args[start].apply(this, arguments);
      while (i--) result = args[i].call(this, result);
      return result;
    };
  };

  // Returns a function that will only be executed after being called N times.
  _.after = function(times, func) {
    return function() {
      if (--times < 1) {
        return func.apply(this, arguments);
      }
    };
  };

  // Returns a function that will only be executed before being called N times.
  _.before = function(times, func) {
    var memo;
    return function() {
      if (--times > 0) {
        memo = func.apply(this, arguments);
      } else {
        func = null;
      }
      return memo;
    };
  };

  // Returns a function that will be executed at most one time, no matter how
  // often you call it. Useful for lazy initialization.
  _.once = _.partial(_.before, 2);

  // Object Functions
  // ----------------

  // Retrieve the names of an object's properties.
  // Delegates to **ECMAScript 5**'s native `Object.keys`
  _.keys = function(obj) {
    if (!_.isObject(obj)) return [];
    if (nativeKeys) return nativeKeys(obj);
    var keys = [];
    for (var key in obj) if (_.has(obj, key)) keys.push(key);
    return keys;
  };

  // Retrieve the values of an object's properties.
  _.values = function(obj) {
    var keys = _.keys(obj);
    var length = keys.length;
    var values = Array(length);
    for (var i = 0; i < length; i++) {
      values[i] = obj[keys[i]];
    }
    return values;
  };

  // Convert an object into a list of `[key, value]` pairs.
  _.pairs = function(obj) {
    var keys = _.keys(obj);
    var length = keys.length;
    var pairs = Array(length);
    for (var i = 0; i < length; i++) {
      pairs[i] = [keys[i], obj[keys[i]]];
    }
    return pairs;
  };

  // Invert the keys and values of an object. The values must be serializable.
  _.invert = function(obj) {
    var result = {};
    var keys = _.keys(obj);
    for (var i = 0, length = keys.length; i < length; i++) {
      result[obj[keys[i]]] = keys[i];
    }
    return result;
  };

  // Return a sorted list of the function names available on the object.
  // Aliased as `methods`
  _.functions = _.methods = function(obj) {
    var names = [];
    for (var key in obj) {
      if (_.isFunction(obj[key])) names.push(key);
    }
    return names.sort();
  };

  // Extend a given object with all the properties in passed-in object(s).
  _.extend = function(obj) {
    if (!_.isObject(obj)) return obj;
    var source, prop;
    for (var i = 1, length = arguments.length; i < length; i++) {
      source = arguments[i];
      for (prop in source) {
        if (hasOwnProperty.call(source, prop)) {
            obj[prop] = source[prop];
        }
      }
    }
    return obj;
  };

  // Return a copy of the object only containing the whitelisted properties.
  _.pick = function(obj, iteratee, context) {
    var result = {}, key;
    if (obj == null) return result;
    if (_.isFunction(iteratee)) {
      iteratee = createCallback(iteratee, context);
      for (key in obj) {
        var value = obj[key];
        if (iteratee(value, key, obj)) result[key] = value;
      }
    } else {
      var keys = concat.apply([], slice.call(arguments, 1));
      obj = new Object(obj);
      for (var i = 0, length = keys.length; i < length; i++) {
        key = keys[i];
        if (key in obj) result[key] = obj[key];
      }
    }
    return result;
  };

   // Return a copy of the object without the blacklisted properties.
  _.omit = function(obj, iteratee, context) {
    if (_.isFunction(iteratee)) {
      iteratee = _.negate(iteratee);
    } else {
      var keys = _.map(concat.apply([], slice.call(arguments, 1)), String);
      iteratee = function(value, key) {
        return !_.contains(keys, key);
      };
    }
    return _.pick(obj, iteratee, context);
  };

  // Fill in a given object with default properties.
  _.defaults = function(obj) {
    if (!_.isObject(obj)) return obj;
    for (var i = 1, length = arguments.length; i < length; i++) {
      var source = arguments[i];
      for (var prop in source) {
        if (obj[prop] === void 0) obj[prop] = source[prop];
      }
    }
    return obj;
  };

  // Create a (shallow-cloned) duplicate of an object.
  _.clone = function(obj) {
    if (!_.isObject(obj)) return obj;
    return _.isArray(obj) ? obj.slice() : _.extend({}, obj);
  };

  // Invokes interceptor with the obj, and then returns obj.
  // The primary purpose of this method is to "tap into" a method chain, in
  // order to perform operations on intermediate results within the chain.
  _.tap = function(obj, interceptor) {
    interceptor(obj);
    return obj;
  };

  // Internal recursive comparison function for `isEqual`.
  var eq = function(a, b, aStack, bStack) {
    // Identical objects are equal. `0 === -0`, but they aren't identical.
    // See the [Harmony `egal` proposal](http://wiki.ecmascript.org/doku.php?id=harmony:egal).
    if (a === b) return a !== 0 || 1 / a === 1 / b;
    // A strict comparison is necessary because `null == undefined`.
    if (a == null || b == null) return a === b;
    // Unwrap any wrapped objects.
    if (a instanceof _) a = a._wrapped;
    if (b instanceof _) b = b._wrapped;
    // Compare `[[Class]]` names.
    var className = toString.call(a);
    if (className !== toString.call(b)) return false;
    switch (className) {
      // Strings, numbers, regular expressions, dates, and booleans are compared by value.
      case '[object RegExp]':
      // RegExps are coerced to strings for comparison (Note: '' + /a/i === '/a/i')
      case '[object String]':
        // Primitives and their corresponding object wrappers are equivalent; thus, `"5"` is
        // equivalent to `new String("5")`.
        return '' + a === '' + b;
      case '[object Number]':
        // `NaN`s are equivalent, but non-reflexive.
        // Object(NaN) is equivalent to NaN
        if (+a !== +a) return +b !== +b;
        // An `egal` comparison is performed for other numeric values.
        return +a === 0 ? 1 / +a === 1 / b : +a === +b;
      case '[object Date]':
      case '[object Boolean]':
        // Coerce dates and booleans to numeric primitive values. Dates are compared by their
        // millisecond representations. Note that invalid dates with millisecond representations
        // of `NaN` are not equivalent.
        return +a === +b;
    }
    if (typeof a != 'object' || typeof b != 'object') return false;
    // Assume equality for cyclic structures. The algorithm for detecting cyclic
    // structures is adapted from ES 5.1 section 15.12.3, abstract operation `JO`.
    var length = aStack.length;
    while (length--) {
      // Linear search. Performance is inversely proportional to the number of
      // unique nested structures.
      if (aStack[length] === a) return bStack[length] === b;
    }
    // Objects with different constructors are not equivalent, but `Object`s
    // from different frames are.
    var aCtor = a.constructor, bCtor = b.constructor;
    if (
      aCtor !== bCtor &&
      // Handle Object.create(x) cases
      'constructor' in a && 'constructor' in b &&
      !(_.isFunction(aCtor) && aCtor instanceof aCtor &&
        _.isFunction(bCtor) && bCtor instanceof bCtor)
    ) {
      return false;
    }
    // Add the first object to the stack of traversed objects.
    aStack.push(a);
    bStack.push(b);
    var size, result;
    // Recursively compare objects and arrays.
    if (className === '[object Array]') {
      // Compare array lengths to determine if a deep comparison is necessary.
      size = a.length;
      result = size === b.length;
      if (result) {
        // Deep compare the contents, ignoring non-numeric properties.
        while (size--) {
          if (!(result = eq(a[size], b[size], aStack, bStack))) break;
        }
      }
    } else {
      // Deep compare objects.
      var keys = _.keys(a), key;
      size = keys.length;
      // Ensure that both objects contain the same number of properties before comparing deep equality.
      result = _.keys(b).length === size;
      if (result) {
        while (size--) {
          // Deep compare each member
          key = keys[size];
          if (!(result = _.has(b, key) && eq(a[key], b[key], aStack, bStack))) break;
        }
      }
    }
    // Remove the first object from the stack of traversed objects.
    aStack.pop();
    bStack.pop();
    return result;
  };

  // Perform a deep comparison to check if two objects are equal.
  _.isEqual = function(a, b) {
    return eq(a, b, [], []);
  };

  // Is a given array, string, or object empty?
  // An "empty" object has no enumerable own-properties.
  _.isEmpty = function(obj) {
    if (obj == null) return true;
    if (_.isArray(obj) || _.isString(obj) || _.isArguments(obj)) return obj.length === 0;
    for (var key in obj) if (_.has(obj, key)) return false;
    return true;
  };

  // Is a given value a DOM element?
  _.isElement = function(obj) {
    return !!(obj && obj.nodeType === 1);
  };

  // Is a given value an array?
  // Delegates to ECMA5's native Array.isArray
  _.isArray = nativeIsArray || function(obj) {
    return toString.call(obj) === '[object Array]';
  };

  // Is a given variable an object?
  _.isObject = function(obj) {
    var type = typeof obj;
    return type === 'function' || type === 'object' && !!obj;
  };

  // Add some isType methods: isArguments, isFunction, isString, isNumber, isDate, isRegExp.
  _.each(['Arguments', 'Function', 'String', 'Number', 'Date', 'RegExp'], function(name) {
    _['is' + name] = function(obj) {
      return toString.call(obj) === '[object ' + name + ']';
    };
  });

  // Define a fallback version of the method in browsers (ahem, IE), where
  // there isn't any inspectable "Arguments" type.
  if (!_.isArguments(arguments)) {
    _.isArguments = function(obj) {
      return _.has(obj, 'callee');
    };
  }

  // Optimize `isFunction` if appropriate. Work around an IE 11 bug.
  if (typeof /./ !== 'function') {
    _.isFunction = function(obj) {
      return typeof obj == 'function' || false;
    };
  }

  // Is a given object a finite number?
  _.isFinite = function(obj) {
    return isFinite(obj) && !isNaN(parseFloat(obj));
  };

  // Is the given value `NaN`? (NaN is the only number which does not equal itself).
  _.isNaN = function(obj) {
    return _.isNumber(obj) && obj !== +obj;
  };

  // Is a given value a boolean?
  _.isBoolean = function(obj) {
    return obj === true || obj === false || toString.call(obj) === '[object Boolean]';
  };

  // Is a given value equal to null?
  _.isNull = function(obj) {
    return obj === null;
  };

  // Is a given variable undefined?
  _.isUndefined = function(obj) {
    return obj === void 0;
  };

  // Shortcut function for checking if an object has a given property directly
  // on itself (in other words, not on a prototype).
  _.has = function(obj, key) {
    return obj != null && hasOwnProperty.call(obj, key);
  };

  // Utility Functions
  // -----------------

  // Run Underscore.js in *noConflict* mode, returning the `_` variable to its
  // previous owner. Returns a reference to the Underscore object.
  _.noConflict = function() {
    root._ = previousUnderscore;
    return this;
  };

  // Keep the identity function around for default iteratees.
  _.identity = function(value) {
    return value;
  };

  _.constant = function(value) {
    return function() {
      return value;
    };
  };

  _.noop = function(){};

  _.property = function(key) {
    return function(obj) {
      return obj[key];
    };
  };

  // Returns a predicate for checking whether an object has a given set of `key:value` pairs.
  _.matches = function(attrs) {
    var pairs = _.pairs(attrs), length = pairs.length;
    return function(obj) {
      if (obj == null) return !length;
      obj = new Object(obj);
      for (var i = 0; i < length; i++) {
        var pair = pairs[i], key = pair[0];
        if (pair[1] !== obj[key] || !(key in obj)) return false;
      }
      return true;
    };
  };

  // Run a function **n** times.
  _.times = function(n, iteratee, context) {
    var accum = Array(Math.max(0, n));
    iteratee = createCallback(iteratee, context, 1);
    for (var i = 0; i < n; i++) accum[i] = iteratee(i);
    return accum;
  };

  // Return a random integer between min and max (inclusive).
  _.random = function(min, max) {
    if (max == null) {
      max = min;
      min = 0;
    }
    return min + Math.floor(Math.random() * (max - min + 1));
  };

  // A (possibly faster) way to get the current timestamp as an integer.
  _.now = Date.now || function() {
    return new Date().getTime();
  };

   // List of HTML entities for escaping.
  var escapeMap = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '`': '&#x60;'
  };
  var unescapeMap = _.invert(escapeMap);

  // Functions for escaping and unescaping strings to/from HTML interpolation.
  var createEscaper = function(map) {
    var escaper = function(match) {
      return map[match];
    };
    // Regexes for identifying a key that needs to be escaped
    var source = '(?:' + _.keys(map).join('|') + ')';
    var testRegexp = RegExp(source);
    var replaceRegexp = RegExp(source, 'g');
    return function(string) {
      string = string == null ? '' : '' + string;
      return testRegexp.test(string) ? string.replace(replaceRegexp, escaper) : string;
    };
  };
  _.escape = createEscaper(escapeMap);
  _.unescape = createEscaper(unescapeMap);

  // If the value of the named `property` is a function then invoke it with the
  // `object` as context; otherwise, return it.
  _.result = function(object, property) {
    if (object == null) return void 0;
    var value = object[property];
    return _.isFunction(value) ? object[property]() : value;
  };

  // Generate a unique integer id (unique within the entire client session).
  // Useful for temporary DOM ids.
  var idCounter = 0;
  _.uniqueId = function(prefix) {
    var id = ++idCounter + '';
    return prefix ? prefix + id : id;
  };

  // By default, Underscore uses ERB-style template delimiters, change the
  // following template settings to use alternative delimiters.
  _.templateSettings = {
    evaluate    : /<%([\s\S]+?)%>/g,
    interpolate : /<%=([\s\S]+?)%>/g,
    escape      : /<%-([\s\S]+?)%>/g
  };

  // When customizing `templateSettings`, if you don't want to define an
  // interpolation, evaluation or escaping regex, we need one that is
  // guaranteed not to match.
  var noMatch = /(.)^/;

  // Certain characters need to be escaped so that they can be put into a
  // string literal.
  var escapes = {
    "'":      "'",
    '\\':     '\\',
    '\r':     'r',
    '\n':     'n',
    '\u2028': 'u2028',
    '\u2029': 'u2029'
  };

  var escaper = /\\|'|\r|\n|\u2028|\u2029/g;

  var escapeChar = function(match) {
    return '\\' + escapes[match];
  };

  // JavaScript micro-templating, similar to John Resig's implementation.
  // Underscore templating handles arbitrary delimiters, preserves whitespace,
  // and correctly escapes quotes within interpolated code.
  // NB: `oldSettings` only exists for backwards compatibility.
  _.template = function(text, settings, oldSettings) {
    if (!settings && oldSettings) settings = oldSettings;
    settings = _.defaults({}, settings, _.templateSettings);

    // Combine delimiters into one regular expression via alternation.
    var matcher = RegExp([
      (settings.escape || noMatch).source,
      (settings.interpolate || noMatch).source,
      (settings.evaluate || noMatch).source
    ].join('|') + '|$', 'g');

    // Compile the template source, escaping string literals appropriately.
    var index = 0;
    var source = "__p+='";
    text.replace(matcher, function(match, escape, interpolate, evaluate, offset) {
      source += text.slice(index, offset).replace(escaper, escapeChar);
      index = offset + match.length;

      if (escape) {
        source += "'+\n((__t=(" + escape + "))==null?'':_.escape(__t))+\n'";
      } else if (interpolate) {
        source += "'+\n((__t=(" + interpolate + "))==null?'':__t)+\n'";
      } else if (evaluate) {
        source += "';\n" + evaluate + "\n__p+='";
      }

      // Adobe VMs need the match returned to produce the correct offest.
      return match;
    });
    source += "';\n";

    // If a variable is not specified, place data values in local scope.
    if (!settings.variable) source = 'with(obj||{}){\n' + source + '}\n';

    source = "var __t,__p='',__j=Array.prototype.join," +
      "print=function(){__p+=__j.call(arguments,'');};\n" +
      source + 'return __p;\n';

    try {
      var render = new Function(settings.variable || 'obj', '_', source);
    } catch (e) {
      e.source = source;
      throw e;
    }

    var template = function(data) {
      return render.call(this, data, _);
    };

    // Provide the compiled source as a convenience for precompilation.
    var argument = settings.variable || 'obj';
    template.source = 'function(' + argument + '){\n' + source + '}';

    return template;
  };

  // Add a "chain" function. Start chaining a wrapped Underscore object.
  _.chain = function(obj) {
    var instance = _(obj);
    instance._chain = true;
    return instance;
  };

  // OOP
  // ---------------
  // If Underscore is called as a function, it returns a wrapped object that
  // can be used OO-style. This wrapper holds altered versions of all the
  // underscore functions. Wrapped objects may be chained.

  // Helper function to continue chaining intermediate results.
  var result = function(obj) {
    return this._chain ? _(obj).chain() : obj;
  };

  // Add your own custom functions to the Underscore object.
  _.mixin = function(obj) {
    _.each(_.functions(obj), function(name) {
      var func = _[name] = obj[name];
      _.prototype[name] = function() {
        var args = [this._wrapped];
        push.apply(args, arguments);
        return result.call(this, func.apply(_, args));
      };
    });
  };

  // Add all of the Underscore functions to the wrapper object.
  _.mixin(_);

  // Add all mutator Array functions to the wrapper.
  _.each(['pop', 'push', 'reverse', 'shift', 'sort', 'splice', 'unshift'], function(name) {
    var method = ArrayProto[name];
    _.prototype[name] = function() {
      var obj = this._wrapped;
      method.apply(obj, arguments);
      if ((name === 'shift' || name === 'splice') && obj.length === 0) delete obj[0];
      return result.call(this, obj);
    };
  });

  // Add all accessor Array functions to the wrapper.
  _.each(['concat', 'join', 'slice'], function(name) {
    var method = ArrayProto[name];
    _.prototype[name] = function() {
      return result.call(this, method.apply(this._wrapped, arguments));
    };
  });

  // Extracts the result from a wrapped and chained object.
  _.prototype.value = function() {
    return this._wrapped;
  };

  // AMD registration happens at the end for compatibility with AMD loaders
  // that may not enforce next-turn semantics on modules. Even though general
  // practice for AMD registration is to be anonymous, underscore registers
  // as a named module because, like jQuery, it is a base library that is
  // popular enough to be bundled in a third party lib, but not be part of
  // an AMD load request. Those cases could generate an error when an
  // anonymous define() is called outside of a loader request.
  if (typeof define === 'function' && define.amd) {
    define('underscore', [], function() {
      return _;
    });
  }
}.call(this));

},{}],8:[function(require,module,exports){
(function (Buffer){
(function () {
  "use strict";

  function atob(str) {
    return new Buffer(str, 'base64').toString('binary');
  }

  module.exports = atob;
}());

}).call(this,require("buffer").Buffer)
},{"buffer":1}],9:[function(require,module,exports){
(function (Buffer){
(function () {
  "use strict";

  function btoa(str) {
    var buffer
      ;

    if (str instanceof Buffer) {
      buffer = str;
    } else {
      buffer = new Buffer(str.toString(), 'binary');
    }

    return buffer.toString('base64');
  }

  module.exports = btoa;
}());

}).call(this,require("buffer").Buffer)
},{"buffer":1}],10:[function(require,module,exports){
module.exports=require(5)
},{"/Coding/wisebed/wisegui/node_modules/jquery/dist/jquery.js":5}],11:[function(require,module,exports){
(function (global){
//! moment.js
//! version : 2.8.3
//! authors : Tim Wood, Iskren Chernev, Moment.js contributors
//! license : MIT
//! momentjs.com

(function (undefined) {
    /************************************
        Constants
    ************************************/

    var moment,
        VERSION = '2.8.3',
        // the global-scope this is NOT the global object in Node.js
        globalScope = typeof global !== 'undefined' ? global : this,
        oldGlobalMoment,
        round = Math.round,
        hasOwnProperty = Object.prototype.hasOwnProperty,
        i,

        YEAR = 0,
        MONTH = 1,
        DATE = 2,
        HOUR = 3,
        MINUTE = 4,
        SECOND = 5,
        MILLISECOND = 6,

        // internal storage for locale config files
        locales = {},

        // extra moment internal properties (plugins register props here)
        momentProperties = [],

        // check for nodeJS
        hasModule = (typeof module !== 'undefined' && module.exports),

        // ASP.NET json date format regex
        aspNetJsonRegex = /^\/?Date\((\-?\d+)/i,
        aspNetTimeSpanJsonRegex = /(\-)?(?:(\d*)\.)?(\d+)\:(\d+)(?:\:(\d+)\.?(\d{3})?)?/,

        // from http://docs.closure-library.googlecode.com/git/closure_goog_date_date.js.source.html
        // somewhat more in line with 4.4.3.2 2004 spec, but allows decimal anywhere
        isoDurationRegex = /^(-)?P(?:(?:([0-9,.]*)Y)?(?:([0-9,.]*)M)?(?:([0-9,.]*)D)?(?:T(?:([0-9,.]*)H)?(?:([0-9,.]*)M)?(?:([0-9,.]*)S)?)?|([0-9,.]*)W)$/,

        // format tokens
        formattingTokens = /(\[[^\[]*\])|(\\)?(Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Q|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|mm?|ss?|S{1,4}|X|zz?|ZZ?|.)/g,
        localFormattingTokens = /(\[[^\[]*\])|(\\)?(LT|LL?L?L?|l{1,4})/g,

        // parsing token regexes
        parseTokenOneOrTwoDigits = /\d\d?/, // 0 - 99
        parseTokenOneToThreeDigits = /\d{1,3}/, // 0 - 999
        parseTokenOneToFourDigits = /\d{1,4}/, // 0 - 9999
        parseTokenOneToSixDigits = /[+\-]?\d{1,6}/, // -999,999 - 999,999
        parseTokenDigits = /\d+/, // nonzero number of digits
        parseTokenWord = /[0-9]*['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+|[\u0600-\u06FF\/]+(\s*?[\u0600-\u06FF]+){1,2}/i, // any word (or two) characters or numbers including two/three word month in arabic.
        parseTokenTimezone = /Z|[\+\-]\d\d:?\d\d/gi, // +00:00 -00:00 +0000 -0000 or Z
        parseTokenT = /T/i, // T (ISO separator)
        parseTokenTimestampMs = /[\+\-]?\d+(\.\d{1,3})?/, // 123456789 123456789.123
        parseTokenOrdinal = /\d{1,2}/,

        //strict parsing regexes
        parseTokenOneDigit = /\d/, // 0 - 9
        parseTokenTwoDigits = /\d\d/, // 00 - 99
        parseTokenThreeDigits = /\d{3}/, // 000 - 999
        parseTokenFourDigits = /\d{4}/, // 0000 - 9999
        parseTokenSixDigits = /[+-]?\d{6}/, // -999,999 - 999,999
        parseTokenSignedNumber = /[+-]?\d+/, // -inf - inf

        // iso 8601 regex
        // 0000-00-00 0000-W00 or 0000-W00-0 + T + 00 or 00:00 or 00:00:00 or 00:00:00.000 + +00:00 or +0000 or +00)
        isoRegex = /^\s*(?:[+-]\d{6}|\d{4})-(?:(\d\d-\d\d)|(W\d\d$)|(W\d\d-\d)|(\d\d\d))((T| )(\d\d(:\d\d(:\d\d(\.\d+)?)?)?)?([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,

        isoFormat = 'YYYY-MM-DDTHH:mm:ssZ',

        isoDates = [
            ['YYYYYY-MM-DD', /[+-]\d{6}-\d{2}-\d{2}/],
            ['YYYY-MM-DD', /\d{4}-\d{2}-\d{2}/],
            ['GGGG-[W]WW-E', /\d{4}-W\d{2}-\d/],
            ['GGGG-[W]WW', /\d{4}-W\d{2}/],
            ['YYYY-DDD', /\d{4}-\d{3}/]
        ],

        // iso time formats and regexes
        isoTimes = [
            ['HH:mm:ss.SSSS', /(T| )\d\d:\d\d:\d\d\.\d+/],
            ['HH:mm:ss', /(T| )\d\d:\d\d:\d\d/],
            ['HH:mm', /(T| )\d\d:\d\d/],
            ['HH', /(T| )\d\d/]
        ],

        // timezone chunker '+10:00' > ['10', '00'] or '-1530' > ['-15', '30']
        parseTimezoneChunker = /([\+\-]|\d\d)/gi,

        // getter and setter names
        proxyGettersAndSetters = 'Date|Hours|Minutes|Seconds|Milliseconds'.split('|'),
        unitMillisecondFactors = {
            'Milliseconds' : 1,
            'Seconds' : 1e3,
            'Minutes' : 6e4,
            'Hours' : 36e5,
            'Days' : 864e5,
            'Months' : 2592e6,
            'Years' : 31536e6
        },

        unitAliases = {
            ms : 'millisecond',
            s : 'second',
            m : 'minute',
            h : 'hour',
            d : 'day',
            D : 'date',
            w : 'week',
            W : 'isoWeek',
            M : 'month',
            Q : 'quarter',
            y : 'year',
            DDD : 'dayOfYear',
            e : 'weekday',
            E : 'isoWeekday',
            gg: 'weekYear',
            GG: 'isoWeekYear'
        },

        camelFunctions = {
            dayofyear : 'dayOfYear',
            isoweekday : 'isoWeekday',
            isoweek : 'isoWeek',
            weekyear : 'weekYear',
            isoweekyear : 'isoWeekYear'
        },

        // format function strings
        formatFunctions = {},

        // default relative time thresholds
        relativeTimeThresholds = {
            s: 45,  // seconds to minute
            m: 45,  // minutes to hour
            h: 22,  // hours to day
            d: 26,  // days to month
            M: 11   // months to year
        },

        // tokens to ordinalize and pad
        ordinalizeTokens = 'DDD w W M D d'.split(' '),
        paddedTokens = 'M D H h m s w W'.split(' '),

        formatTokenFunctions = {
            M    : function () {
                return this.month() + 1;
            },
            MMM  : function (format) {
                return this.localeData().monthsShort(this, format);
            },
            MMMM : function (format) {
                return this.localeData().months(this, format);
            },
            D    : function () {
                return this.date();
            },
            DDD  : function () {
                return this.dayOfYear();
            },
            d    : function () {
                return this.day();
            },
            dd   : function (format) {
                return this.localeData().weekdaysMin(this, format);
            },
            ddd  : function (format) {
                return this.localeData().weekdaysShort(this, format);
            },
            dddd : function (format) {
                return this.localeData().weekdays(this, format);
            },
            w    : function () {
                return this.week();
            },
            W    : function () {
                return this.isoWeek();
            },
            YY   : function () {
                return leftZeroFill(this.year() % 100, 2);
            },
            YYYY : function () {
                return leftZeroFill(this.year(), 4);
            },
            YYYYY : function () {
                return leftZeroFill(this.year(), 5);
            },
            YYYYYY : function () {
                var y = this.year(), sign = y >= 0 ? '+' : '-';
                return sign + leftZeroFill(Math.abs(y), 6);
            },
            gg   : function () {
                return leftZeroFill(this.weekYear() % 100, 2);
            },
            gggg : function () {
                return leftZeroFill(this.weekYear(), 4);
            },
            ggggg : function () {
                return leftZeroFill(this.weekYear(), 5);
            },
            GG   : function () {
                return leftZeroFill(this.isoWeekYear() % 100, 2);
            },
            GGGG : function () {
                return leftZeroFill(this.isoWeekYear(), 4);
            },
            GGGGG : function () {
                return leftZeroFill(this.isoWeekYear(), 5);
            },
            e : function () {
                return this.weekday();
            },
            E : function () {
                return this.isoWeekday();
            },
            a    : function () {
                return this.localeData().meridiem(this.hours(), this.minutes(), true);
            },
            A    : function () {
                return this.localeData().meridiem(this.hours(), this.minutes(), false);
            },
            H    : function () {
                return this.hours();
            },
            h    : function () {
                return this.hours() % 12 || 12;
            },
            m    : function () {
                return this.minutes();
            },
            s    : function () {
                return this.seconds();
            },
            S    : function () {
                return toInt(this.milliseconds() / 100);
            },
            SS   : function () {
                return leftZeroFill(toInt(this.milliseconds() / 10), 2);
            },
            SSS  : function () {
                return leftZeroFill(this.milliseconds(), 3);
            },
            SSSS : function () {
                return leftZeroFill(this.milliseconds(), 3);
            },
            Z    : function () {
                var a = -this.zone(),
                    b = '+';
                if (a < 0) {
                    a = -a;
                    b = '-';
                }
                return b + leftZeroFill(toInt(a / 60), 2) + ':' + leftZeroFill(toInt(a) % 60, 2);
            },
            ZZ   : function () {
                var a = -this.zone(),
                    b = '+';
                if (a < 0) {
                    a = -a;
                    b = '-';
                }
                return b + leftZeroFill(toInt(a / 60), 2) + leftZeroFill(toInt(a) % 60, 2);
            },
            z : function () {
                return this.zoneAbbr();
            },
            zz : function () {
                return this.zoneName();
            },
            X    : function () {
                return this.unix();
            },
            Q : function () {
                return this.quarter();
            }
        },

        deprecations = {},

        lists = ['months', 'monthsShort', 'weekdays', 'weekdaysShort', 'weekdaysMin'];

    // Pick the first defined of two or three arguments. dfl comes from
    // default.
    function dfl(a, b, c) {
        switch (arguments.length) {
            case 2: return a != null ? a : b;
            case 3: return a != null ? a : b != null ? b : c;
            default: throw new Error('Implement me');
        }
    }

    function hasOwnProp(a, b) {
        return hasOwnProperty.call(a, b);
    }

    function defaultParsingFlags() {
        // We need to deep clone this object, and es5 standard is not very
        // helpful.
        return {
            empty : false,
            unusedTokens : [],
            unusedInput : [],
            overflow : -2,
            charsLeftOver : 0,
            nullInput : false,
            invalidMonth : null,
            invalidFormat : false,
            userInvalidated : false,
            iso: false
        };
    }

    function printMsg(msg) {
        if (moment.suppressDeprecationWarnings === false &&
                typeof console !== 'undefined' && console.warn) {
            console.warn('Deprecation warning: ' + msg);
        }
    }

    function deprecate(msg, fn) {
        var firstTime = true;
        return extend(function () {
            if (firstTime) {
                printMsg(msg);
                firstTime = false;
            }
            return fn.apply(this, arguments);
        }, fn);
    }

    function deprecateSimple(name, msg) {
        if (!deprecations[name]) {
            printMsg(msg);
            deprecations[name] = true;
        }
    }

    function padToken(func, count) {
        return function (a) {
            return leftZeroFill(func.call(this, a), count);
        };
    }
    function ordinalizeToken(func, period) {
        return function (a) {
            return this.localeData().ordinal(func.call(this, a), period);
        };
    }

    while (ordinalizeTokens.length) {
        i = ordinalizeTokens.pop();
        formatTokenFunctions[i + 'o'] = ordinalizeToken(formatTokenFunctions[i], i);
    }
    while (paddedTokens.length) {
        i = paddedTokens.pop();
        formatTokenFunctions[i + i] = padToken(formatTokenFunctions[i], 2);
    }
    formatTokenFunctions.DDDD = padToken(formatTokenFunctions.DDD, 3);


    /************************************
        Constructors
    ************************************/

    function Locale() {
    }

    // Moment prototype object
    function Moment(config, skipOverflow) {
        if (skipOverflow !== false) {
            checkOverflow(config);
        }
        copyConfig(this, config);
        this._d = new Date(+config._d);
    }

    // Duration Constructor
    function Duration(duration) {
        var normalizedInput = normalizeObjectUnits(duration),
            years = normalizedInput.year || 0,
            quarters = normalizedInput.quarter || 0,
            months = normalizedInput.month || 0,
            weeks = normalizedInput.week || 0,
            days = normalizedInput.day || 0,
            hours = normalizedInput.hour || 0,
            minutes = normalizedInput.minute || 0,
            seconds = normalizedInput.second || 0,
            milliseconds = normalizedInput.millisecond || 0;

        // representation for dateAddRemove
        this._milliseconds = +milliseconds +
            seconds * 1e3 + // 1000
            minutes * 6e4 + // 1000 * 60
            hours * 36e5; // 1000 * 60 * 60
        // Because of dateAddRemove treats 24 hours as different from a
        // day when working around DST, we need to store them separately
        this._days = +days +
            weeks * 7;
        // It is impossible translate months into days without knowing
        // which months you are are talking about, so we have to store
        // it separately.
        this._months = +months +
            quarters * 3 +
            years * 12;

        this._data = {};

        this._locale = moment.localeData();

        this._bubble();
    }

    /************************************
        Helpers
    ************************************/


    function extend(a, b) {
        for (var i in b) {
            if (hasOwnProp(b, i)) {
                a[i] = b[i];
            }
        }

        if (hasOwnProp(b, 'toString')) {
            a.toString = b.toString;
        }

        if (hasOwnProp(b, 'valueOf')) {
            a.valueOf = b.valueOf;
        }

        return a;
    }

    function copyConfig(to, from) {
        var i, prop, val;

        if (typeof from._isAMomentObject !== 'undefined') {
            to._isAMomentObject = from._isAMomentObject;
        }
        if (typeof from._i !== 'undefined') {
            to._i = from._i;
        }
        if (typeof from._f !== 'undefined') {
            to._f = from._f;
        }
        if (typeof from._l !== 'undefined') {
            to._l = from._l;
        }
        if (typeof from._strict !== 'undefined') {
            to._strict = from._strict;
        }
        if (typeof from._tzm !== 'undefined') {
            to._tzm = from._tzm;
        }
        if (typeof from._isUTC !== 'undefined') {
            to._isUTC = from._isUTC;
        }
        if (typeof from._offset !== 'undefined') {
            to._offset = from._offset;
        }
        if (typeof from._pf !== 'undefined') {
            to._pf = from._pf;
        }
        if (typeof from._locale !== 'undefined') {
            to._locale = from._locale;
        }

        if (momentProperties.length > 0) {
            for (i in momentProperties) {
                prop = momentProperties[i];
                val = from[prop];
                if (typeof val !== 'undefined') {
                    to[prop] = val;
                }
            }
        }

        return to;
    }

    function absRound(number) {
        if (number < 0) {
            return Math.ceil(number);
        } else {
            return Math.floor(number);
        }
    }

    // left zero fill a number
    // see http://jsperf.com/left-zero-filling for performance comparison
    function leftZeroFill(number, targetLength, forceSign) {
        var output = '' + Math.abs(number),
            sign = number >= 0;

        while (output.length < targetLength) {
            output = '0' + output;
        }
        return (sign ? (forceSign ? '+' : '') : '-') + output;
    }

    function positiveMomentsDifference(base, other) {
        var res = {milliseconds: 0, months: 0};

        res.months = other.month() - base.month() +
            (other.year() - base.year()) * 12;
        if (base.clone().add(res.months, 'M').isAfter(other)) {
            --res.months;
        }

        res.milliseconds = +other - +(base.clone().add(res.months, 'M'));

        return res;
    }

    function momentsDifference(base, other) {
        var res;
        other = makeAs(other, base);
        if (base.isBefore(other)) {
            res = positiveMomentsDifference(base, other);
        } else {
            res = positiveMomentsDifference(other, base);
            res.milliseconds = -res.milliseconds;
            res.months = -res.months;
        }

        return res;
    }

    // TODO: remove 'name' arg after deprecation is removed
    function createAdder(direction, name) {
        return function (val, period) {
            var dur, tmp;
            //invert the arguments, but complain about it
            if (period !== null && !isNaN(+period)) {
                deprecateSimple(name, 'moment().' + name  + '(period, number) is deprecated. Please use moment().' + name + '(number, period).');
                tmp = val; val = period; period = tmp;
            }

            val = typeof val === 'string' ? +val : val;
            dur = moment.duration(val, period);
            addOrSubtractDurationFromMoment(this, dur, direction);
            return this;
        };
    }

    function addOrSubtractDurationFromMoment(mom, duration, isAdding, updateOffset) {
        var milliseconds = duration._milliseconds,
            days = duration._days,
            months = duration._months;
        updateOffset = updateOffset == null ? true : updateOffset;

        if (milliseconds) {
            mom._d.setTime(+mom._d + milliseconds * isAdding);
        }
        if (days) {
            rawSetter(mom, 'Date', rawGetter(mom, 'Date') + days * isAdding);
        }
        if (months) {
            rawMonthSetter(mom, rawGetter(mom, 'Month') + months * isAdding);
        }
        if (updateOffset) {
            moment.updateOffset(mom, days || months);
        }
    }

    // check if is an array
    function isArray(input) {
        return Object.prototype.toString.call(input) === '[object Array]';
    }

    function isDate(input) {
        return Object.prototype.toString.call(input) === '[object Date]' ||
            input instanceof Date;
    }

    // compare two arrays, return the number of differences
    function compareArrays(array1, array2, dontConvert) {
        var len = Math.min(array1.length, array2.length),
            lengthDiff = Math.abs(array1.length - array2.length),
            diffs = 0,
            i;
        for (i = 0; i < len; i++) {
            if ((dontConvert && array1[i] !== array2[i]) ||
                (!dontConvert && toInt(array1[i]) !== toInt(array2[i]))) {
                diffs++;
            }
        }
        return diffs + lengthDiff;
    }

    function normalizeUnits(units) {
        if (units) {
            var lowered = units.toLowerCase().replace(/(.)s$/, '$1');
            units = unitAliases[units] || camelFunctions[lowered] || lowered;
        }
        return units;
    }

    function normalizeObjectUnits(inputObject) {
        var normalizedInput = {},
            normalizedProp,
            prop;

        for (prop in inputObject) {
            if (hasOwnProp(inputObject, prop)) {
                normalizedProp = normalizeUnits(prop);
                if (normalizedProp) {
                    normalizedInput[normalizedProp] = inputObject[prop];
                }
            }
        }

        return normalizedInput;
    }

    function makeList(field) {
        var count, setter;

        if (field.indexOf('week') === 0) {
            count = 7;
            setter = 'day';
        }
        else if (field.indexOf('month') === 0) {
            count = 12;
            setter = 'month';
        }
        else {
            return;
        }

        moment[field] = function (format, index) {
            var i, getter,
                method = moment._locale[field],
                results = [];

            if (typeof format === 'number') {
                index = format;
                format = undefined;
            }

            getter = function (i) {
                var m = moment().utc().set(setter, i);
                return method.call(moment._locale, m, format || '');
            };

            if (index != null) {
                return getter(index);
            }
            else {
                for (i = 0; i < count; i++) {
                    results.push(getter(i));
                }
                return results;
            }
        };
    }

    function toInt(argumentForCoercion) {
        var coercedNumber = +argumentForCoercion,
            value = 0;

        if (coercedNumber !== 0 && isFinite(coercedNumber)) {
            if (coercedNumber >= 0) {
                value = Math.floor(coercedNumber);
            } else {
                value = Math.ceil(coercedNumber);
            }
        }

        return value;
    }

    function daysInMonth(year, month) {
        return new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
    }

    function weeksInYear(year, dow, doy) {
        return weekOfYear(moment([year, 11, 31 + dow - doy]), dow, doy).week;
    }

    function daysInYear(year) {
        return isLeapYear(year) ? 366 : 365;
    }

    function isLeapYear(year) {
        return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
    }

    function checkOverflow(m) {
        var overflow;
        if (m._a && m._pf.overflow === -2) {
            overflow =
                m._a[MONTH] < 0 || m._a[MONTH] > 11 ? MONTH :
                m._a[DATE] < 1 || m._a[DATE] > daysInMonth(m._a[YEAR], m._a[MONTH]) ? DATE :
                m._a[HOUR] < 0 || m._a[HOUR] > 23 ? HOUR :
                m._a[MINUTE] < 0 || m._a[MINUTE] > 59 ? MINUTE :
                m._a[SECOND] < 0 || m._a[SECOND] > 59 ? SECOND :
                m._a[MILLISECOND] < 0 || m._a[MILLISECOND] > 999 ? MILLISECOND :
                -1;

            if (m._pf._overflowDayOfYear && (overflow < YEAR || overflow > DATE)) {
                overflow = DATE;
            }

            m._pf.overflow = overflow;
        }
    }

    function isValid(m) {
        if (m._isValid == null) {
            m._isValid = !isNaN(m._d.getTime()) &&
                m._pf.overflow < 0 &&
                !m._pf.empty &&
                !m._pf.invalidMonth &&
                !m._pf.nullInput &&
                !m._pf.invalidFormat &&
                !m._pf.userInvalidated;

            if (m._strict) {
                m._isValid = m._isValid &&
                    m._pf.charsLeftOver === 0 &&
                    m._pf.unusedTokens.length === 0;
            }
        }
        return m._isValid;
    }

    function normalizeLocale(key) {
        return key ? key.toLowerCase().replace('_', '-') : key;
    }

    // pick the locale from the array
    // try ['en-au', 'en-gb'] as 'en-au', 'en-gb', 'en', as in move through the list trying each
    // substring from most specific to least, but move to the next array item if it's a more specific variant than the current root
    function chooseLocale(names) {
        var i = 0, j, next, locale, split;

        while (i < names.length) {
            split = normalizeLocale(names[i]).split('-');
            j = split.length;
            next = normalizeLocale(names[i + 1]);
            next = next ? next.split('-') : null;
            while (j > 0) {
                locale = loadLocale(split.slice(0, j).join('-'));
                if (locale) {
                    return locale;
                }
                if (next && next.length >= j && compareArrays(split, next, true) >= j - 1) {
                    //the next array item is better than a shallower substring of this one
                    break;
                }
                j--;
            }
            i++;
        }
        return null;
    }

    function loadLocale(name) {
        var oldLocale = null;
        if (!locales[name] && hasModule) {
            try {
                oldLocale = moment.locale();
                require('./locale/' + name);
                // because defineLocale currently also sets the global locale, we want to undo that for lazy loaded locales
                moment.locale(oldLocale);
            } catch (e) { }
        }
        return locales[name];
    }

    // Return a moment from input, that is local/utc/zone equivalent to model.
    function makeAs(input, model) {
        return model._isUTC ? moment(input).zone(model._offset || 0) :
            moment(input).local();
    }

    /************************************
        Locale
    ************************************/


    extend(Locale.prototype, {

        set : function (config) {
            var prop, i;
            for (i in config) {
                prop = config[i];
                if (typeof prop === 'function') {
                    this[i] = prop;
                } else {
                    this['_' + i] = prop;
                }
            }
        },

        _months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        months : function (m) {
            return this._months[m.month()];
        },

        _monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        monthsShort : function (m) {
            return this._monthsShort[m.month()];
        },

        monthsParse : function (monthName) {
            var i, mom, regex;

            if (!this._monthsParse) {
                this._monthsParse = [];
            }

            for (i = 0; i < 12; i++) {
                // make the regex if we don't have it already
                if (!this._monthsParse[i]) {
                    mom = moment.utc([2000, i]);
                    regex = '^' + this.months(mom, '') + '|^' + this.monthsShort(mom, '');
                    this._monthsParse[i] = new RegExp(regex.replace('.', ''), 'i');
                }
                // test the regex
                if (this._monthsParse[i].test(monthName)) {
                    return i;
                }
            }
        },

        _weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdays : function (m) {
            return this._weekdays[m.day()];
        },

        _weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysShort : function (m) {
            return this._weekdaysShort[m.day()];
        },

        _weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        weekdaysMin : function (m) {
            return this._weekdaysMin[m.day()];
        },

        weekdaysParse : function (weekdayName) {
            var i, mom, regex;

            if (!this._weekdaysParse) {
                this._weekdaysParse = [];
            }

            for (i = 0; i < 7; i++) {
                // make the regex if we don't have it already
                if (!this._weekdaysParse[i]) {
                    mom = moment([2000, 1]).day(i);
                    regex = '^' + this.weekdays(mom, '') + '|^' + this.weekdaysShort(mom, '') + '|^' + this.weekdaysMin(mom, '');
                    this._weekdaysParse[i] = new RegExp(regex.replace('.', ''), 'i');
                }
                // test the regex
                if (this._weekdaysParse[i].test(weekdayName)) {
                    return i;
                }
            }
        },

        _longDateFormat : {
            LT : 'h:mm A',
            L : 'MM/DD/YYYY',
            LL : 'MMMM D, YYYY',
            LLL : 'MMMM D, YYYY LT',
            LLLL : 'dddd, MMMM D, YYYY LT'
        },
        longDateFormat : function (key) {
            var output = this._longDateFormat[key];
            if (!output && this._longDateFormat[key.toUpperCase()]) {
                output = this._longDateFormat[key.toUpperCase()].replace(/MMMM|MM|DD|dddd/g, function (val) {
                    return val.slice(1);
                });
                this._longDateFormat[key] = output;
            }
            return output;
        },

        isPM : function (input) {
            // IE8 Quirks Mode & IE7 Standards Mode do not allow accessing strings like arrays
            // Using charAt should be more compatible.
            return ((input + '').toLowerCase().charAt(0) === 'p');
        },

        _meridiemParse : /[ap]\.?m?\.?/i,
        meridiem : function (hours, minutes, isLower) {
            if (hours > 11) {
                return isLower ? 'pm' : 'PM';
            } else {
                return isLower ? 'am' : 'AM';
            }
        },

        _calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        calendar : function (key, mom) {
            var output = this._calendar[key];
            return typeof output === 'function' ? output.apply(mom) : output;
        },

        _relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },

        relativeTime : function (number, withoutSuffix, string, isFuture) {
            var output = this._relativeTime[string];
            return (typeof output === 'function') ?
                output(number, withoutSuffix, string, isFuture) :
                output.replace(/%d/i, number);
        },

        pastFuture : function (diff, output) {
            var format = this._relativeTime[diff > 0 ? 'future' : 'past'];
            return typeof format === 'function' ? format(output) : format.replace(/%s/i, output);
        },

        ordinal : function (number) {
            return this._ordinal.replace('%d', number);
        },
        _ordinal : '%d',

        preparse : function (string) {
            return string;
        },

        postformat : function (string) {
            return string;
        },

        week : function (mom) {
            return weekOfYear(mom, this._week.dow, this._week.doy).week;
        },

        _week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 1st is the first week of the year.
        },

        _invalidDate: 'Invalid date',
        invalidDate: function () {
            return this._invalidDate;
        }
    });

    /************************************
        Formatting
    ************************************/


    function removeFormattingTokens(input) {
        if (input.match(/\[[\s\S]/)) {
            return input.replace(/^\[|\]$/g, '');
        }
        return input.replace(/\\/g, '');
    }

    function makeFormatFunction(format) {
        var array = format.match(formattingTokens), i, length;

        for (i = 0, length = array.length; i < length; i++) {
            if (formatTokenFunctions[array[i]]) {
                array[i] = formatTokenFunctions[array[i]];
            } else {
                array[i] = removeFormattingTokens(array[i]);
            }
        }

        return function (mom) {
            var output = '';
            for (i = 0; i < length; i++) {
                output += array[i] instanceof Function ? array[i].call(mom, format) : array[i];
            }
            return output;
        };
    }

    // format date using native date object
    function formatMoment(m, format) {
        if (!m.isValid()) {
            return m.localeData().invalidDate();
        }

        format = expandFormat(format, m.localeData());

        if (!formatFunctions[format]) {
            formatFunctions[format] = makeFormatFunction(format);
        }

        return formatFunctions[format](m);
    }

    function expandFormat(format, locale) {
        var i = 5;

        function replaceLongDateFormatTokens(input) {
            return locale.longDateFormat(input) || input;
        }

        localFormattingTokens.lastIndex = 0;
        while (i >= 0 && localFormattingTokens.test(format)) {
            format = format.replace(localFormattingTokens, replaceLongDateFormatTokens);
            localFormattingTokens.lastIndex = 0;
            i -= 1;
        }

        return format;
    }


    /************************************
        Parsing
    ************************************/


    // get the regex to find the next token
    function getParseRegexForToken(token, config) {
        var a, strict = config._strict;
        switch (token) {
        case 'Q':
            return parseTokenOneDigit;
        case 'DDDD':
            return parseTokenThreeDigits;
        case 'YYYY':
        case 'GGGG':
        case 'gggg':
            return strict ? parseTokenFourDigits : parseTokenOneToFourDigits;
        case 'Y':
        case 'G':
        case 'g':
            return parseTokenSignedNumber;
        case 'YYYYYY':
        case 'YYYYY':
        case 'GGGGG':
        case 'ggggg':
            return strict ? parseTokenSixDigits : parseTokenOneToSixDigits;
        case 'S':
            if (strict) {
                return parseTokenOneDigit;
            }
            /* falls through */
        case 'SS':
            if (strict) {
                return parseTokenTwoDigits;
            }
            /* falls through */
        case 'SSS':
            if (strict) {
                return parseTokenThreeDigits;
            }
            /* falls through */
        case 'DDD':
            return parseTokenOneToThreeDigits;
        case 'MMM':
        case 'MMMM':
        case 'dd':
        case 'ddd':
        case 'dddd':
            return parseTokenWord;
        case 'a':
        case 'A':
            return config._locale._meridiemParse;
        case 'X':
            return parseTokenTimestampMs;
        case 'Z':
        case 'ZZ':
            return parseTokenTimezone;
        case 'T':
            return parseTokenT;
        case 'SSSS':
            return parseTokenDigits;
        case 'MM':
        case 'DD':
        case 'YY':
        case 'GG':
        case 'gg':
        case 'HH':
        case 'hh':
        case 'mm':
        case 'ss':
        case 'ww':
        case 'WW':
            return strict ? parseTokenTwoDigits : parseTokenOneOrTwoDigits;
        case 'M':
        case 'D':
        case 'd':
        case 'H':
        case 'h':
        case 'm':
        case 's':
        case 'w':
        case 'W':
        case 'e':
        case 'E':
            return parseTokenOneOrTwoDigits;
        case 'Do':
            return parseTokenOrdinal;
        default :
            a = new RegExp(regexpEscape(unescapeFormat(token.replace('\\', '')), 'i'));
            return a;
        }
    }

    function timezoneMinutesFromString(string) {
        string = string || '';
        var possibleTzMatches = (string.match(parseTokenTimezone) || []),
            tzChunk = possibleTzMatches[possibleTzMatches.length - 1] || [],
            parts = (tzChunk + '').match(parseTimezoneChunker) || ['-', 0, 0],
            minutes = +(parts[1] * 60) + toInt(parts[2]);

        return parts[0] === '+' ? -minutes : minutes;
    }

    // function to convert string input to date
    function addTimeToArrayFromToken(token, input, config) {
        var a, datePartArray = config._a;

        switch (token) {
        // QUARTER
        case 'Q':
            if (input != null) {
                datePartArray[MONTH] = (toInt(input) - 1) * 3;
            }
            break;
        // MONTH
        case 'M' : // fall through to MM
        case 'MM' :
            if (input != null) {
                datePartArray[MONTH] = toInt(input) - 1;
            }
            break;
        case 'MMM' : // fall through to MMMM
        case 'MMMM' :
            a = config._locale.monthsParse(input);
            // if we didn't find a month name, mark the date as invalid.
            if (a != null) {
                datePartArray[MONTH] = a;
            } else {
                config._pf.invalidMonth = input;
            }
            break;
        // DAY OF MONTH
        case 'D' : // fall through to DD
        case 'DD' :
            if (input != null) {
                datePartArray[DATE] = toInt(input);
            }
            break;
        case 'Do' :
            if (input != null) {
                datePartArray[DATE] = toInt(parseInt(input, 10));
            }
            break;
        // DAY OF YEAR
        case 'DDD' : // fall through to DDDD
        case 'DDDD' :
            if (input != null) {
                config._dayOfYear = toInt(input);
            }

            break;
        // YEAR
        case 'YY' :
            datePartArray[YEAR] = moment.parseTwoDigitYear(input);
            break;
        case 'YYYY' :
        case 'YYYYY' :
        case 'YYYYYY' :
            datePartArray[YEAR] = toInt(input);
            break;
        // AM / PM
        case 'a' : // fall through to A
        case 'A' :
            config._isPm = config._locale.isPM(input);
            break;
        // 24 HOUR
        case 'H' : // fall through to hh
        case 'HH' : // fall through to hh
        case 'h' : // fall through to hh
        case 'hh' :
            datePartArray[HOUR] = toInt(input);
            break;
        // MINUTE
        case 'm' : // fall through to mm
        case 'mm' :
            datePartArray[MINUTE] = toInt(input);
            break;
        // SECOND
        case 's' : // fall through to ss
        case 'ss' :
            datePartArray[SECOND] = toInt(input);
            break;
        // MILLISECOND
        case 'S' :
        case 'SS' :
        case 'SSS' :
        case 'SSSS' :
            datePartArray[MILLISECOND] = toInt(('0.' + input) * 1000);
            break;
        // UNIX TIMESTAMP WITH MS
        case 'X':
            config._d = new Date(parseFloat(input) * 1000);
            break;
        // TIMEZONE
        case 'Z' : // fall through to ZZ
        case 'ZZ' :
            config._useUTC = true;
            config._tzm = timezoneMinutesFromString(input);
            break;
        // WEEKDAY - human
        case 'dd':
        case 'ddd':
        case 'dddd':
            a = config._locale.weekdaysParse(input);
            // if we didn't get a weekday name, mark the date as invalid
            if (a != null) {
                config._w = config._w || {};
                config._w['d'] = a;
            } else {
                config._pf.invalidWeekday = input;
            }
            break;
        // WEEK, WEEK DAY - numeric
        case 'w':
        case 'ww':
        case 'W':
        case 'WW':
        case 'd':
        case 'e':
        case 'E':
            token = token.substr(0, 1);
            /* falls through */
        case 'gggg':
        case 'GGGG':
        case 'GGGGG':
            token = token.substr(0, 2);
            if (input) {
                config._w = config._w || {};
                config._w[token] = toInt(input);
            }
            break;
        case 'gg':
        case 'GG':
            config._w = config._w || {};
            config._w[token] = moment.parseTwoDigitYear(input);
        }
    }

    function dayOfYearFromWeekInfo(config) {
        var w, weekYear, week, weekday, dow, doy, temp;

        w = config._w;
        if (w.GG != null || w.W != null || w.E != null) {
            dow = 1;
            doy = 4;

            // TODO: We need to take the current isoWeekYear, but that depends on
            // how we interpret now (local, utc, fixed offset). So create
            // a now version of current config (take local/utc/offset flags, and
            // create now).
            weekYear = dfl(w.GG, config._a[YEAR], weekOfYear(moment(), 1, 4).year);
            week = dfl(w.W, 1);
            weekday = dfl(w.E, 1);
        } else {
            dow = config._locale._week.dow;
            doy = config._locale._week.doy;

            weekYear = dfl(w.gg, config._a[YEAR], weekOfYear(moment(), dow, doy).year);
            week = dfl(w.w, 1);

            if (w.d != null) {
                // weekday -- low day numbers are considered next week
                weekday = w.d;
                if (weekday < dow) {
                    ++week;
                }
            } else if (w.e != null) {
                // local weekday -- counting starts from begining of week
                weekday = w.e + dow;
            } else {
                // default to begining of week
                weekday = dow;
            }
        }
        temp = dayOfYearFromWeeks(weekYear, week, weekday, doy, dow);

        config._a[YEAR] = temp.year;
        config._dayOfYear = temp.dayOfYear;
    }

    // convert an array to a date.
    // the array should mirror the parameters below
    // note: all values past the year are optional and will default to the lowest possible value.
    // [year, month, day , hour, minute, second, millisecond]
    function dateFromConfig(config) {
        var i, date, input = [], currentDate, yearToUse;

        if (config._d) {
            return;
        }

        currentDate = currentDateArray(config);

        //compute day of the year from weeks and weekdays
        if (config._w && config._a[DATE] == null && config._a[MONTH] == null) {
            dayOfYearFromWeekInfo(config);
        }

        //if the day of the year is set, figure out what it is
        if (config._dayOfYear) {
            yearToUse = dfl(config._a[YEAR], currentDate[YEAR]);

            if (config._dayOfYear > daysInYear(yearToUse)) {
                config._pf._overflowDayOfYear = true;
            }

            date = makeUTCDate(yearToUse, 0, config._dayOfYear);
            config._a[MONTH] = date.getUTCMonth();
            config._a[DATE] = date.getUTCDate();
        }

        // Default to current date.
        // * if no year, month, day of month are given, default to today
        // * if day of month is given, default month and year
        // * if month is given, default only year
        // * if year is given, don't default anything
        for (i = 0; i < 3 && config._a[i] == null; ++i) {
            config._a[i] = input[i] = currentDate[i];
        }

        // Zero out whatever was not defaulted, including time
        for (; i < 7; i++) {
            config._a[i] = input[i] = (config._a[i] == null) ? (i === 2 ? 1 : 0) : config._a[i];
        }

        config._d = (config._useUTC ? makeUTCDate : makeDate).apply(null, input);
        // Apply timezone offset from input. The actual zone can be changed
        // with parseZone.
        if (config._tzm != null) {
            config._d.setUTCMinutes(config._d.getUTCMinutes() + config._tzm);
        }
    }

    function dateFromObject(config) {
        var normalizedInput;

        if (config._d) {
            return;
        }

        normalizedInput = normalizeObjectUnits(config._i);
        config._a = [
            normalizedInput.year,
            normalizedInput.month,
            normalizedInput.day,
            normalizedInput.hour,
            normalizedInput.minute,
            normalizedInput.second,
            normalizedInput.millisecond
        ];

        dateFromConfig(config);
    }

    function currentDateArray(config) {
        var now = new Date();
        if (config._useUTC) {
            return [
                now.getUTCFullYear(),
                now.getUTCMonth(),
                now.getUTCDate()
            ];
        } else {
            return [now.getFullYear(), now.getMonth(), now.getDate()];
        }
    }

    // date from string and format string
    function makeDateFromStringAndFormat(config) {
        if (config._f === moment.ISO_8601) {
            parseISO(config);
            return;
        }

        config._a = [];
        config._pf.empty = true;

        // This array is used to make a Date, either with `new Date` or `Date.UTC`
        var string = '' + config._i,
            i, parsedInput, tokens, token, skipped,
            stringLength = string.length,
            totalParsedInputLength = 0;

        tokens = expandFormat(config._f, config._locale).match(formattingTokens) || [];

        for (i = 0; i < tokens.length; i++) {
            token = tokens[i];
            parsedInput = (string.match(getParseRegexForToken(token, config)) || [])[0];
            if (parsedInput) {
                skipped = string.substr(0, string.indexOf(parsedInput));
                if (skipped.length > 0) {
                    config._pf.unusedInput.push(skipped);
                }
                string = string.slice(string.indexOf(parsedInput) + parsedInput.length);
                totalParsedInputLength += parsedInput.length;
            }
            // don't parse if it's not a known token
            if (formatTokenFunctions[token]) {
                if (parsedInput) {
                    config._pf.empty = false;
                }
                else {
                    config._pf.unusedTokens.push(token);
                }
                addTimeToArrayFromToken(token, parsedInput, config);
            }
            else if (config._strict && !parsedInput) {
                config._pf.unusedTokens.push(token);
            }
        }

        // add remaining unparsed input length to the string
        config._pf.charsLeftOver = stringLength - totalParsedInputLength;
        if (string.length > 0) {
            config._pf.unusedInput.push(string);
        }

        // handle am pm
        if (config._isPm && config._a[HOUR] < 12) {
            config._a[HOUR] += 12;
        }
        // if is 12 am, change hours to 0
        if (config._isPm === false && config._a[HOUR] === 12) {
            config._a[HOUR] = 0;
        }

        dateFromConfig(config);
        checkOverflow(config);
    }

    function unescapeFormat(s) {
        return s.replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g, function (matched, p1, p2, p3, p4) {
            return p1 || p2 || p3 || p4;
        });
    }

    // Code from http://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
    function regexpEscape(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    }

    // date from string and array of format strings
    function makeDateFromStringAndArray(config) {
        var tempConfig,
            bestMoment,

            scoreToBeat,
            i,
            currentScore;

        if (config._f.length === 0) {
            config._pf.invalidFormat = true;
            config._d = new Date(NaN);
            return;
        }

        for (i = 0; i < config._f.length; i++) {
            currentScore = 0;
            tempConfig = copyConfig({}, config);
            if (config._useUTC != null) {
                tempConfig._useUTC = config._useUTC;
            }
            tempConfig._pf = defaultParsingFlags();
            tempConfig._f = config._f[i];
            makeDateFromStringAndFormat(tempConfig);

            if (!isValid(tempConfig)) {
                continue;
            }

            // if there is any input that was not parsed add a penalty for that format
            currentScore += tempConfig._pf.charsLeftOver;

            //or tokens
            currentScore += tempConfig._pf.unusedTokens.length * 10;

            tempConfig._pf.score = currentScore;

            if (scoreToBeat == null || currentScore < scoreToBeat) {
                scoreToBeat = currentScore;
                bestMoment = tempConfig;
            }
        }

        extend(config, bestMoment || tempConfig);
    }

    // date from iso format
    function parseISO(config) {
        var i, l,
            string = config._i,
            match = isoRegex.exec(string);

        if (match) {
            config._pf.iso = true;
            for (i = 0, l = isoDates.length; i < l; i++) {
                if (isoDates[i][1].exec(string)) {
                    // match[5] should be 'T' or undefined
                    config._f = isoDates[i][0] + (match[6] || ' ');
                    break;
                }
            }
            for (i = 0, l = isoTimes.length; i < l; i++) {
                if (isoTimes[i][1].exec(string)) {
                    config._f += isoTimes[i][0];
                    break;
                }
            }
            if (string.match(parseTokenTimezone)) {
                config._f += 'Z';
            }
            makeDateFromStringAndFormat(config);
        } else {
            config._isValid = false;
        }
    }

    // date from iso format or fallback
    function makeDateFromString(config) {
        parseISO(config);
        if (config._isValid === false) {
            delete config._isValid;
            moment.createFromInputFallback(config);
        }
    }

    function map(arr, fn) {
        var res = [], i;
        for (i = 0; i < arr.length; ++i) {
            res.push(fn(arr[i], i));
        }
        return res;
    }

    function makeDateFromInput(config) {
        var input = config._i, matched;
        if (input === undefined) {
            config._d = new Date();
        } else if (isDate(input)) {
            config._d = new Date(+input);
        } else if ((matched = aspNetJsonRegex.exec(input)) !== null) {
            config._d = new Date(+matched[1]);
        } else if (typeof input === 'string') {
            makeDateFromString(config);
        } else if (isArray(input)) {
            config._a = map(input.slice(0), function (obj) {
                return parseInt(obj, 10);
            });
            dateFromConfig(config);
        } else if (typeof(input) === 'object') {
            dateFromObject(config);
        } else if (typeof(input) === 'number') {
            // from milliseconds
            config._d = new Date(input);
        } else {
            moment.createFromInputFallback(config);
        }
    }

    function makeDate(y, m, d, h, M, s, ms) {
        //can't just apply() to create a date:
        //http://stackoverflow.com/questions/181348/instantiating-a-javascript-object-by-calling-prototype-constructor-apply
        var date = new Date(y, m, d, h, M, s, ms);

        //the date constructor doesn't accept years < 1970
        if (y < 1970) {
            date.setFullYear(y);
        }
        return date;
    }

    function makeUTCDate(y) {
        var date = new Date(Date.UTC.apply(null, arguments));
        if (y < 1970) {
            date.setUTCFullYear(y);
        }
        return date;
    }

    function parseWeekday(input, locale) {
        if (typeof input === 'string') {
            if (!isNaN(input)) {
                input = parseInt(input, 10);
            }
            else {
                input = locale.weekdaysParse(input);
                if (typeof input !== 'number') {
                    return null;
                }
            }
        }
        return input;
    }

    /************************************
        Relative Time
    ************************************/


    // helper function for moment.fn.from, moment.fn.fromNow, and moment.duration.fn.humanize
    function substituteTimeAgo(string, number, withoutSuffix, isFuture, locale) {
        return locale.relativeTime(number || 1, !!withoutSuffix, string, isFuture);
    }

    function relativeTime(posNegDuration, withoutSuffix, locale) {
        var duration = moment.duration(posNegDuration).abs(),
            seconds = round(duration.as('s')),
            minutes = round(duration.as('m')),
            hours = round(duration.as('h')),
            days = round(duration.as('d')),
            months = round(duration.as('M')),
            years = round(duration.as('y')),

            args = seconds < relativeTimeThresholds.s && ['s', seconds] ||
                minutes === 1 && ['m'] ||
                minutes < relativeTimeThresholds.m && ['mm', minutes] ||
                hours === 1 && ['h'] ||
                hours < relativeTimeThresholds.h && ['hh', hours] ||
                days === 1 && ['d'] ||
                days < relativeTimeThresholds.d && ['dd', days] ||
                months === 1 && ['M'] ||
                months < relativeTimeThresholds.M && ['MM', months] ||
                years === 1 && ['y'] || ['yy', years];

        args[2] = withoutSuffix;
        args[3] = +posNegDuration > 0;
        args[4] = locale;
        return substituteTimeAgo.apply({}, args);
    }


    /************************************
        Week of Year
    ************************************/


    // firstDayOfWeek       0 = sun, 6 = sat
    //                      the day of the week that starts the week
    //                      (usually sunday or monday)
    // firstDayOfWeekOfYear 0 = sun, 6 = sat
    //                      the first week is the week that contains the first
    //                      of this day of the week
    //                      (eg. ISO weeks use thursday (4))
    function weekOfYear(mom, firstDayOfWeek, firstDayOfWeekOfYear) {
        var end = firstDayOfWeekOfYear - firstDayOfWeek,
            daysToDayOfWeek = firstDayOfWeekOfYear - mom.day(),
            adjustedMoment;


        if (daysToDayOfWeek > end) {
            daysToDayOfWeek -= 7;
        }

        if (daysToDayOfWeek < end - 7) {
            daysToDayOfWeek += 7;
        }

        adjustedMoment = moment(mom).add(daysToDayOfWeek, 'd');
        return {
            week: Math.ceil(adjustedMoment.dayOfYear() / 7),
            year: adjustedMoment.year()
        };
    }

    //http://en.wikipedia.org/wiki/ISO_week_date#Calculating_a_date_given_the_year.2C_week_number_and_weekday
    function dayOfYearFromWeeks(year, week, weekday, firstDayOfWeekOfYear, firstDayOfWeek) {
        var d = makeUTCDate(year, 0, 1).getUTCDay(), daysToAdd, dayOfYear;

        d = d === 0 ? 7 : d;
        weekday = weekday != null ? weekday : firstDayOfWeek;
        daysToAdd = firstDayOfWeek - d + (d > firstDayOfWeekOfYear ? 7 : 0) - (d < firstDayOfWeek ? 7 : 0);
        dayOfYear = 7 * (week - 1) + (weekday - firstDayOfWeek) + daysToAdd + 1;

        return {
            year: dayOfYear > 0 ? year : year - 1,
            dayOfYear: dayOfYear > 0 ?  dayOfYear : daysInYear(year - 1) + dayOfYear
        };
    }

    /************************************
        Top Level Functions
    ************************************/

    function makeMoment(config) {
        var input = config._i,
            format = config._f;

        config._locale = config._locale || moment.localeData(config._l);

        if (input === null || (format === undefined && input === '')) {
            return moment.invalid({nullInput: true});
        }

        if (typeof input === 'string') {
            config._i = input = config._locale.preparse(input);
        }

        if (moment.isMoment(input)) {
            return new Moment(input, true);
        } else if (format) {
            if (isArray(format)) {
                makeDateFromStringAndArray(config);
            } else {
                makeDateFromStringAndFormat(config);
            }
        } else {
            makeDateFromInput(config);
        }

        return new Moment(config);
    }

    moment = function (input, format, locale, strict) {
        var c;

        if (typeof(locale) === 'boolean') {
            strict = locale;
            locale = undefined;
        }
        // object construction must be done this way.
        // https://github.com/moment/moment/issues/1423
        c = {};
        c._isAMomentObject = true;
        c._i = input;
        c._f = format;
        c._l = locale;
        c._strict = strict;
        c._isUTC = false;
        c._pf = defaultParsingFlags();

        return makeMoment(c);
    };

    moment.suppressDeprecationWarnings = false;

    moment.createFromInputFallback = deprecate(
        'moment construction falls back to js Date. This is ' +
        'discouraged and will be removed in upcoming major ' +
        'release. Please refer to ' +
        'https://github.com/moment/moment/issues/1407 for more info.',
        function (config) {
            config._d = new Date(config._i);
        }
    );

    // Pick a moment m from moments so that m[fn](other) is true for all
    // other. This relies on the function fn to be transitive.
    //
    // moments should either be an array of moment objects or an array, whose
    // first element is an array of moment objects.
    function pickBy(fn, moments) {
        var res, i;
        if (moments.length === 1 && isArray(moments[0])) {
            moments = moments[0];
        }
        if (!moments.length) {
            return moment();
        }
        res = moments[0];
        for (i = 1; i < moments.length; ++i) {
            if (moments[i][fn](res)) {
                res = moments[i];
            }
        }
        return res;
    }

    moment.min = function () {
        var args = [].slice.call(arguments, 0);

        return pickBy('isBefore', args);
    };

    moment.max = function () {
        var args = [].slice.call(arguments, 0);

        return pickBy('isAfter', args);
    };

    // creating with utc
    moment.utc = function (input, format, locale, strict) {
        var c;

        if (typeof(locale) === 'boolean') {
            strict = locale;
            locale = undefined;
        }
        // object construction must be done this way.
        // https://github.com/moment/moment/issues/1423
        c = {};
        c._isAMomentObject = true;
        c._useUTC = true;
        c._isUTC = true;
        c._l = locale;
        c._i = input;
        c._f = format;
        c._strict = strict;
        c._pf = defaultParsingFlags();

        return makeMoment(c).utc();
    };

    // creating with unix timestamp (in seconds)
    moment.unix = function (input) {
        return moment(input * 1000);
    };

    // duration
    moment.duration = function (input, key) {
        var duration = input,
            // matching against regexp is expensive, do it on demand
            match = null,
            sign,
            ret,
            parseIso,
            diffRes;

        if (moment.isDuration(input)) {
            duration = {
                ms: input._milliseconds,
                d: input._days,
                M: input._months
            };
        } else if (typeof input === 'number') {
            duration = {};
            if (key) {
                duration[key] = input;
            } else {
                duration.milliseconds = input;
            }
        } else if (!!(match = aspNetTimeSpanJsonRegex.exec(input))) {
            sign = (match[1] === '-') ? -1 : 1;
            duration = {
                y: 0,
                d: toInt(match[DATE]) * sign,
                h: toInt(match[HOUR]) * sign,
                m: toInt(match[MINUTE]) * sign,
                s: toInt(match[SECOND]) * sign,
                ms: toInt(match[MILLISECOND]) * sign
            };
        } else if (!!(match = isoDurationRegex.exec(input))) {
            sign = (match[1] === '-') ? -1 : 1;
            parseIso = function (inp) {
                // We'd normally use ~~inp for this, but unfortunately it also
                // converts floats to ints.
                // inp may be undefined, so careful calling replace on it.
                var res = inp && parseFloat(inp.replace(',', '.'));
                // apply sign while we're at it
                return (isNaN(res) ? 0 : res) * sign;
            };
            duration = {
                y: parseIso(match[2]),
                M: parseIso(match[3]),
                d: parseIso(match[4]),
                h: parseIso(match[5]),
                m: parseIso(match[6]),
                s: parseIso(match[7]),
                w: parseIso(match[8])
            };
        } else if (typeof duration === 'object' &&
                ('from' in duration || 'to' in duration)) {
            diffRes = momentsDifference(moment(duration.from), moment(duration.to));

            duration = {};
            duration.ms = diffRes.milliseconds;
            duration.M = diffRes.months;
        }

        ret = new Duration(duration);

        if (moment.isDuration(input) && hasOwnProp(input, '_locale')) {
            ret._locale = input._locale;
        }

        return ret;
    };

    // version number
    moment.version = VERSION;

    // default format
    moment.defaultFormat = isoFormat;

    // constant that refers to the ISO standard
    moment.ISO_8601 = function () {};

    // Plugins that add properties should also add the key here (null value),
    // so we can properly clone ourselves.
    moment.momentProperties = momentProperties;

    // This function will be called whenever a moment is mutated.
    // It is intended to keep the offset in sync with the timezone.
    moment.updateOffset = function () {};

    // This function allows you to set a threshold for relative time strings
    moment.relativeTimeThreshold = function (threshold, limit) {
        if (relativeTimeThresholds[threshold] === undefined) {
            return false;
        }
        if (limit === undefined) {
            return relativeTimeThresholds[threshold];
        }
        relativeTimeThresholds[threshold] = limit;
        return true;
    };

    moment.lang = deprecate(
        'moment.lang is deprecated. Use moment.locale instead.',
        function (key, value) {
            return moment.locale(key, value);
        }
    );

    // This function will load locale and then set the global locale.  If
    // no arguments are passed in, it will simply return the current global
    // locale key.
    moment.locale = function (key, values) {
        var data;
        if (key) {
            if (typeof(values) !== 'undefined') {
                data = moment.defineLocale(key, values);
            }
            else {
                data = moment.localeData(key);
            }

            if (data) {
                moment.duration._locale = moment._locale = data;
            }
        }

        return moment._locale._abbr;
    };

    moment.defineLocale = function (name, values) {
        if (values !== null) {
            values.abbr = name;
            if (!locales[name]) {
                locales[name] = new Locale();
            }
            locales[name].set(values);

            // backwards compat for now: also set the locale
            moment.locale(name);

            return locales[name];
        } else {
            // useful for testing
            delete locales[name];
            return null;
        }
    };

    moment.langData = deprecate(
        'moment.langData is deprecated. Use moment.localeData instead.',
        function (key) {
            return moment.localeData(key);
        }
    );

    // returns locale data
    moment.localeData = function (key) {
        var locale;

        if (key && key._locale && key._locale._abbr) {
            key = key._locale._abbr;
        }

        if (!key) {
            return moment._locale;
        }

        if (!isArray(key)) {
            //short-circuit everything else
            locale = loadLocale(key);
            if (locale) {
                return locale;
            }
            key = [key];
        }

        return chooseLocale(key);
    };

    // compare moment object
    moment.isMoment = function (obj) {
        return obj instanceof Moment ||
            (obj != null && hasOwnProp(obj, '_isAMomentObject'));
    };

    // for typechecking Duration objects
    moment.isDuration = function (obj) {
        return obj instanceof Duration;
    };

    for (i = lists.length - 1; i >= 0; --i) {
        makeList(lists[i]);
    }

    moment.normalizeUnits = function (units) {
        return normalizeUnits(units);
    };

    moment.invalid = function (flags) {
        var m = moment.utc(NaN);
        if (flags != null) {
            extend(m._pf, flags);
        }
        else {
            m._pf.userInvalidated = true;
        }

        return m;
    };

    moment.parseZone = function () {
        return moment.apply(null, arguments).parseZone();
    };

    moment.parseTwoDigitYear = function (input) {
        return toInt(input) + (toInt(input) > 68 ? 1900 : 2000);
    };

    /************************************
        Moment Prototype
    ************************************/


    extend(moment.fn = Moment.prototype, {

        clone : function () {
            return moment(this);
        },

        valueOf : function () {
            return +this._d + ((this._offset || 0) * 60000);
        },

        unix : function () {
            return Math.floor(+this / 1000);
        },

        toString : function () {
            return this.clone().locale('en').format('ddd MMM DD YYYY HH:mm:ss [GMT]ZZ');
        },

        toDate : function () {
            return this._offset ? new Date(+this) : this._d;
        },

        toISOString : function () {
            var m = moment(this).utc();
            if (0 < m.year() && m.year() <= 9999) {
                return formatMoment(m, 'YYYY-MM-DD[T]HH:mm:ss.SSS[Z]');
            } else {
                return formatMoment(m, 'YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]');
            }
        },

        toArray : function () {
            var m = this;
            return [
                m.year(),
                m.month(),
                m.date(),
                m.hours(),
                m.minutes(),
                m.seconds(),
                m.milliseconds()
            ];
        },

        isValid : function () {
            return isValid(this);
        },

        isDSTShifted : function () {
            if (this._a) {
                return this.isValid() && compareArrays(this._a, (this._isUTC ? moment.utc(this._a) : moment(this._a)).toArray()) > 0;
            }

            return false;
        },

        parsingFlags : function () {
            return extend({}, this._pf);
        },

        invalidAt: function () {
            return this._pf.overflow;
        },

        utc : function (keepLocalTime) {
            return this.zone(0, keepLocalTime);
        },

        local : function (keepLocalTime) {
            if (this._isUTC) {
                this.zone(0, keepLocalTime);
                this._isUTC = false;

                if (keepLocalTime) {
                    this.add(this._dateTzOffset(), 'm');
                }
            }
            return this;
        },

        format : function (inputString) {
            var output = formatMoment(this, inputString || moment.defaultFormat);
            return this.localeData().postformat(output);
        },

        add : createAdder(1, 'add'),

        subtract : createAdder(-1, 'subtract'),

        diff : function (input, units, asFloat) {
            var that = makeAs(input, this),
                zoneDiff = (this.zone() - that.zone()) * 6e4,
                diff, output, daysAdjust;

            units = normalizeUnits(units);

            if (units === 'year' || units === 'month') {
                // average number of days in the months in the given dates
                diff = (this.daysInMonth() + that.daysInMonth()) * 432e5; // 24 * 60 * 60 * 1000 / 2
                // difference in months
                output = ((this.year() - that.year()) * 12) + (this.month() - that.month());
                // adjust by taking difference in days, average number of days
                // and dst in the given months.
                daysAdjust = (this - moment(this).startOf('month')) -
                    (that - moment(that).startOf('month'));
                // same as above but with zones, to negate all dst
                daysAdjust -= ((this.zone() - moment(this).startOf('month').zone()) -
                        (that.zone() - moment(that).startOf('month').zone())) * 6e4;
                output += daysAdjust / diff;
                if (units === 'year') {
                    output = output / 12;
                }
            } else {
                diff = (this - that);
                output = units === 'second' ? diff / 1e3 : // 1000
                    units === 'minute' ? diff / 6e4 : // 1000 * 60
                    units === 'hour' ? diff / 36e5 : // 1000 * 60 * 60
                    units === 'day' ? (diff - zoneDiff) / 864e5 : // 1000 * 60 * 60 * 24, negate dst
                    units === 'week' ? (diff - zoneDiff) / 6048e5 : // 1000 * 60 * 60 * 24 * 7, negate dst
                    diff;
            }
            return asFloat ? output : absRound(output);
        },

        from : function (time, withoutSuffix) {
            return moment.duration({to: this, from: time}).locale(this.locale()).humanize(!withoutSuffix);
        },

        fromNow : function (withoutSuffix) {
            return this.from(moment(), withoutSuffix);
        },

        calendar : function (time) {
            // We want to compare the start of today, vs this.
            // Getting start-of-today depends on whether we're zone'd or not.
            var now = time || moment(),
                sod = makeAs(now, this).startOf('day'),
                diff = this.diff(sod, 'days', true),
                format = diff < -6 ? 'sameElse' :
                    diff < -1 ? 'lastWeek' :
                    diff < 0 ? 'lastDay' :
                    diff < 1 ? 'sameDay' :
                    diff < 2 ? 'nextDay' :
                    diff < 7 ? 'nextWeek' : 'sameElse';
            return this.format(this.localeData().calendar(format, this));
        },

        isLeapYear : function () {
            return isLeapYear(this.year());
        },

        isDST : function () {
            return (this.zone() < this.clone().month(0).zone() ||
                this.zone() < this.clone().month(5).zone());
        },

        day : function (input) {
            var day = this._isUTC ? this._d.getUTCDay() : this._d.getDay();
            if (input != null) {
                input = parseWeekday(input, this.localeData());
                return this.add(input - day, 'd');
            } else {
                return day;
            }
        },

        month : makeAccessor('Month', true),

        startOf : function (units) {
            units = normalizeUnits(units);
            // the following switch intentionally omits break keywords
            // to utilize falling through the cases.
            switch (units) {
            case 'year':
                this.month(0);
                /* falls through */
            case 'quarter':
            case 'month':
                this.date(1);
                /* falls through */
            case 'week':
            case 'isoWeek':
            case 'day':
                this.hours(0);
                /* falls through */
            case 'hour':
                this.minutes(0);
                /* falls through */
            case 'minute':
                this.seconds(0);
                /* falls through */
            case 'second':
                this.milliseconds(0);
                /* falls through */
            }

            // weeks are a special case
            if (units === 'week') {
                this.weekday(0);
            } else if (units === 'isoWeek') {
                this.isoWeekday(1);
            }

            // quarters are also special
            if (units === 'quarter') {
                this.month(Math.floor(this.month() / 3) * 3);
            }

            return this;
        },

        endOf: function (units) {
            units = normalizeUnits(units);
            return this.startOf(units).add(1, (units === 'isoWeek' ? 'week' : units)).subtract(1, 'ms');
        },

        isAfter: function (input, units) {
            units = normalizeUnits(typeof units !== 'undefined' ? units : 'millisecond');
            if (units === 'millisecond') {
                input = moment.isMoment(input) ? input : moment(input);
                return +this > +input;
            } else {
                return +this.clone().startOf(units) > +moment(input).startOf(units);
            }
        },

        isBefore: function (input, units) {
            units = normalizeUnits(typeof units !== 'undefined' ? units : 'millisecond');
            if (units === 'millisecond') {
                input = moment.isMoment(input) ? input : moment(input);
                return +this < +input;
            } else {
                return +this.clone().startOf(units) < +moment(input).startOf(units);
            }
        },

        isSame: function (input, units) {
            units = normalizeUnits(units || 'millisecond');
            if (units === 'millisecond') {
                input = moment.isMoment(input) ? input : moment(input);
                return +this === +input;
            } else {
                return +this.clone().startOf(units) === +makeAs(input, this).startOf(units);
            }
        },

        min: deprecate(
                 'moment().min is deprecated, use moment.min instead. https://github.com/moment/moment/issues/1548',
                 function (other) {
                     other = moment.apply(null, arguments);
                     return other < this ? this : other;
                 }
         ),

        max: deprecate(
                'moment().max is deprecated, use moment.max instead. https://github.com/moment/moment/issues/1548',
                function (other) {
                    other = moment.apply(null, arguments);
                    return other > this ? this : other;
                }
        ),

        // keepLocalTime = true means only change the timezone, without
        // affecting the local hour. So 5:31:26 +0300 --[zone(2, true)]-->
        // 5:31:26 +0200 It is possible that 5:31:26 doesn't exist int zone
        // +0200, so we adjust the time as needed, to be valid.
        //
        // Keeping the time actually adds/subtracts (one hour)
        // from the actual represented time. That is why we call updateOffset
        // a second time. In case it wants us to change the offset again
        // _changeInProgress == true case, then we have to adjust, because
        // there is no such time in the given timezone.
        zone : function (input, keepLocalTime) {
            var offset = this._offset || 0,
                localAdjust;
            if (input != null) {
                if (typeof input === 'string') {
                    input = timezoneMinutesFromString(input);
                }
                if (Math.abs(input) < 16) {
                    input = input * 60;
                }
                if (!this._isUTC && keepLocalTime) {
                    localAdjust = this._dateTzOffset();
                }
                this._offset = input;
                this._isUTC = true;
                if (localAdjust != null) {
                    this.subtract(localAdjust, 'm');
                }
                if (offset !== input) {
                    if (!keepLocalTime || this._changeInProgress) {
                        addOrSubtractDurationFromMoment(this,
                                moment.duration(offset - input, 'm'), 1, false);
                    } else if (!this._changeInProgress) {
                        this._changeInProgress = true;
                        moment.updateOffset(this, true);
                        this._changeInProgress = null;
                    }
                }
            } else {
                return this._isUTC ? offset : this._dateTzOffset();
            }
            return this;
        },

        zoneAbbr : function () {
            return this._isUTC ? 'UTC' : '';
        },

        zoneName : function () {
            return this._isUTC ? 'Coordinated Universal Time' : '';
        },

        parseZone : function () {
            if (this._tzm) {
                this.zone(this._tzm);
            } else if (typeof this._i === 'string') {
                this.zone(this._i);
            }
            return this;
        },

        hasAlignedHourOffset : function (input) {
            if (!input) {
                input = 0;
            }
            else {
                input = moment(input).zone();
            }

            return (this.zone() - input) % 60 === 0;
        },

        daysInMonth : function () {
            return daysInMonth(this.year(), this.month());
        },

        dayOfYear : function (input) {
            var dayOfYear = round((moment(this).startOf('day') - moment(this).startOf('year')) / 864e5) + 1;
            return input == null ? dayOfYear : this.add((input - dayOfYear), 'd');
        },

        quarter : function (input) {
            return input == null ? Math.ceil((this.month() + 1) / 3) : this.month((input - 1) * 3 + this.month() % 3);
        },

        weekYear : function (input) {
            var year = weekOfYear(this, this.localeData()._week.dow, this.localeData()._week.doy).year;
            return input == null ? year : this.add((input - year), 'y');
        },

        isoWeekYear : function (input) {
            var year = weekOfYear(this, 1, 4).year;
            return input == null ? year : this.add((input - year), 'y');
        },

        week : function (input) {
            var week = this.localeData().week(this);
            return input == null ? week : this.add((input - week) * 7, 'd');
        },

        isoWeek : function (input) {
            var week = weekOfYear(this, 1, 4).week;
            return input == null ? week : this.add((input - week) * 7, 'd');
        },

        weekday : function (input) {
            var weekday = (this.day() + 7 - this.localeData()._week.dow) % 7;
            return input == null ? weekday : this.add(input - weekday, 'd');
        },

        isoWeekday : function (input) {
            // behaves the same as moment#day except
            // as a getter, returns 7 instead of 0 (1-7 range instead of 0-6)
            // as a setter, sunday should belong to the previous week.
            return input == null ? this.day() || 7 : this.day(this.day() % 7 ? input : input - 7);
        },

        isoWeeksInYear : function () {
            return weeksInYear(this.year(), 1, 4);
        },

        weeksInYear : function () {
            var weekInfo = this.localeData()._week;
            return weeksInYear(this.year(), weekInfo.dow, weekInfo.doy);
        },

        get : function (units) {
            units = normalizeUnits(units);
            return this[units]();
        },

        set : function (units, value) {
            units = normalizeUnits(units);
            if (typeof this[units] === 'function') {
                this[units](value);
            }
            return this;
        },

        // If passed a locale key, it will set the locale for this
        // instance.  Otherwise, it will return the locale configuration
        // variables for this instance.
        locale : function (key) {
            var newLocaleData;

            if (key === undefined) {
                return this._locale._abbr;
            } else {
                newLocaleData = moment.localeData(key);
                if (newLocaleData != null) {
                    this._locale = newLocaleData;
                }
                return this;
            }
        },

        lang : deprecate(
            'moment().lang() is deprecated. Use moment().localeData() instead.',
            function (key) {
                if (key === undefined) {
                    return this.localeData();
                } else {
                    return this.locale(key);
                }
            }
        ),

        localeData : function () {
            return this._locale;
        },

        _dateTzOffset : function () {
            // On Firefox.24 Date#getTimezoneOffset returns a floating point.
            // https://github.com/moment/moment/pull/1871
            return Math.round(this._d.getTimezoneOffset() / 15) * 15;
        }
    });

    function rawMonthSetter(mom, value) {
        var dayOfMonth;

        // TODO: Move this out of here!
        if (typeof value === 'string') {
            value = mom.localeData().monthsParse(value);
            // TODO: Another silent failure?
            if (typeof value !== 'number') {
                return mom;
            }
        }

        dayOfMonth = Math.min(mom.date(),
                daysInMonth(mom.year(), value));
        mom._d['set' + (mom._isUTC ? 'UTC' : '') + 'Month'](value, dayOfMonth);
        return mom;
    }

    function rawGetter(mom, unit) {
        return mom._d['get' + (mom._isUTC ? 'UTC' : '') + unit]();
    }

    function rawSetter(mom, unit, value) {
        if (unit === 'Month') {
            return rawMonthSetter(mom, value);
        } else {
            return mom._d['set' + (mom._isUTC ? 'UTC' : '') + unit](value);
        }
    }

    function makeAccessor(unit, keepTime) {
        return function (value) {
            if (value != null) {
                rawSetter(this, unit, value);
                moment.updateOffset(this, keepTime);
                return this;
            } else {
                return rawGetter(this, unit);
            }
        };
    }

    moment.fn.millisecond = moment.fn.milliseconds = makeAccessor('Milliseconds', false);
    moment.fn.second = moment.fn.seconds = makeAccessor('Seconds', false);
    moment.fn.minute = moment.fn.minutes = makeAccessor('Minutes', false);
    // Setting the hour should keep the time, because the user explicitly
    // specified which hour he wants. So trying to maintain the same hour (in
    // a new timezone) makes sense. Adding/subtracting hours does not follow
    // this rule.
    moment.fn.hour = moment.fn.hours = makeAccessor('Hours', true);
    // moment.fn.month is defined separately
    moment.fn.date = makeAccessor('Date', true);
    moment.fn.dates = deprecate('dates accessor is deprecated. Use date instead.', makeAccessor('Date', true));
    moment.fn.year = makeAccessor('FullYear', true);
    moment.fn.years = deprecate('years accessor is deprecated. Use year instead.', makeAccessor('FullYear', true));

    // add plural methods
    moment.fn.days = moment.fn.day;
    moment.fn.months = moment.fn.month;
    moment.fn.weeks = moment.fn.week;
    moment.fn.isoWeeks = moment.fn.isoWeek;
    moment.fn.quarters = moment.fn.quarter;

    // add aliased format methods
    moment.fn.toJSON = moment.fn.toISOString;

    /************************************
        Duration Prototype
    ************************************/


    function daysToYears (days) {
        // 400 years have 146097 days (taking into account leap year rules)
        return days * 400 / 146097;
    }

    function yearsToDays (years) {
        // years * 365 + absRound(years / 4) -
        //     absRound(years / 100) + absRound(years / 400);
        return years * 146097 / 400;
    }

    extend(moment.duration.fn = Duration.prototype, {

        _bubble : function () {
            var milliseconds = this._milliseconds,
                days = this._days,
                months = this._months,
                data = this._data,
                seconds, minutes, hours, years = 0;

            // The following code bubbles up values, see the tests for
            // examples of what that means.
            data.milliseconds = milliseconds % 1000;

            seconds = absRound(milliseconds / 1000);
            data.seconds = seconds % 60;

            minutes = absRound(seconds / 60);
            data.minutes = minutes % 60;

            hours = absRound(minutes / 60);
            data.hours = hours % 24;

            days += absRound(hours / 24);

            // Accurately convert days to years, assume start from year 0.
            years = absRound(daysToYears(days));
            days -= absRound(yearsToDays(years));

            // 30 days to a month
            // TODO (iskren): Use anchor date (like 1st Jan) to compute this.
            months += absRound(days / 30);
            days %= 30;

            // 12 months -> 1 year
            years += absRound(months / 12);
            months %= 12;

            data.days = days;
            data.months = months;
            data.years = years;
        },

        abs : function () {
            this._milliseconds = Math.abs(this._milliseconds);
            this._days = Math.abs(this._days);
            this._months = Math.abs(this._months);

            this._data.milliseconds = Math.abs(this._data.milliseconds);
            this._data.seconds = Math.abs(this._data.seconds);
            this._data.minutes = Math.abs(this._data.minutes);
            this._data.hours = Math.abs(this._data.hours);
            this._data.months = Math.abs(this._data.months);
            this._data.years = Math.abs(this._data.years);

            return this;
        },

        weeks : function () {
            return absRound(this.days() / 7);
        },

        valueOf : function () {
            return this._milliseconds +
              this._days * 864e5 +
              (this._months % 12) * 2592e6 +
              toInt(this._months / 12) * 31536e6;
        },

        humanize : function (withSuffix) {
            var output = relativeTime(this, !withSuffix, this.localeData());

            if (withSuffix) {
                output = this.localeData().pastFuture(+this, output);
            }

            return this.localeData().postformat(output);
        },

        add : function (input, val) {
            // supports only 2.0-style add(1, 's') or add(moment)
            var dur = moment.duration(input, val);

            this._milliseconds += dur._milliseconds;
            this._days += dur._days;
            this._months += dur._months;

            this._bubble();

            return this;
        },

        subtract : function (input, val) {
            var dur = moment.duration(input, val);

            this._milliseconds -= dur._milliseconds;
            this._days -= dur._days;
            this._months -= dur._months;

            this._bubble();

            return this;
        },

        get : function (units) {
            units = normalizeUnits(units);
            return this[units.toLowerCase() + 's']();
        },

        as : function (units) {
            var days, months;
            units = normalizeUnits(units);

            if (units === 'month' || units === 'year') {
                days = this._days + this._milliseconds / 864e5;
                months = this._months + daysToYears(days) * 12;
                return units === 'month' ? months : months / 12;
            } else {
                // handle milliseconds separately because of floating point math errors (issue #1867)
                days = this._days + yearsToDays(this._months / 12);
                switch (units) {
                    case 'week': return days / 7 + this._milliseconds / 6048e5;
                    case 'day': return days + this._milliseconds / 864e5;
                    case 'hour': return days * 24 + this._milliseconds / 36e5;
                    case 'minute': return days * 24 * 60 + this._milliseconds / 6e4;
                    case 'second': return days * 24 * 60 * 60 + this._milliseconds / 1000;
                    // Math.floor prevents floating point math errors here
                    case 'millisecond': return Math.floor(days * 24 * 60 * 60 * 1000) + this._milliseconds;
                    default: throw new Error('Unknown unit ' + units);
                }
            }
        },

        lang : moment.fn.lang,
        locale : moment.fn.locale,

        toIsoString : deprecate(
            'toIsoString() is deprecated. Please use toISOString() instead ' +
            '(notice the capitals)',
            function () {
                return this.toISOString();
            }
        ),

        toISOString : function () {
            // inspired by https://github.com/dordille/moment-isoduration/blob/master/moment.isoduration.js
            var years = Math.abs(this.years()),
                months = Math.abs(this.months()),
                days = Math.abs(this.days()),
                hours = Math.abs(this.hours()),
                minutes = Math.abs(this.minutes()),
                seconds = Math.abs(this.seconds() + this.milliseconds() / 1000);

            if (!this.asSeconds()) {
                // this is the same as C#'s (Noda) and python (isodate)...
                // but not other JS (goog.date)
                return 'P0D';
            }

            return (this.asSeconds() < 0 ? '-' : '') +
                'P' +
                (years ? years + 'Y' : '') +
                (months ? months + 'M' : '') +
                (days ? days + 'D' : '') +
                ((hours || minutes || seconds) ? 'T' : '') +
                (hours ? hours + 'H' : '') +
                (minutes ? minutes + 'M' : '') +
                (seconds ? seconds + 'S' : '');
        },

        localeData : function () {
            return this._locale;
        }
    });

    moment.duration.fn.toString = moment.duration.fn.toISOString;

    function makeDurationGetter(name) {
        moment.duration.fn[name] = function () {
            return this._data[name];
        };
    }

    for (i in unitMillisecondFactors) {
        if (hasOwnProp(unitMillisecondFactors, i)) {
            makeDurationGetter(i.toLowerCase());
        }
    }

    moment.duration.fn.asMilliseconds = function () {
        return this.as('ms');
    };
    moment.duration.fn.asSeconds = function () {
        return this.as('s');
    };
    moment.duration.fn.asMinutes = function () {
        return this.as('m');
    };
    moment.duration.fn.asHours = function () {
        return this.as('h');
    };
    moment.duration.fn.asDays = function () {
        return this.as('d');
    };
    moment.duration.fn.asWeeks = function () {
        return this.as('weeks');
    };
    moment.duration.fn.asMonths = function () {
        return this.as('M');
    };
    moment.duration.fn.asYears = function () {
        return this.as('y');
    };

    /************************************
        Default Locale
    ************************************/


    // Set default locale, other locale will inherit from English.
    moment.locale('en', {
        ordinal : function (number) {
            var b = number % 10,
                output = (toInt(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        }
    });

    /* EMBED_LOCALES */

    /************************************
        Exposing Moment
    ************************************/

    function makeGlobal(shouldDeprecate) {
        /*global ender:false */
        if (typeof ender !== 'undefined') {
            return;
        }
        oldGlobalMoment = globalScope.moment;
        if (shouldDeprecate) {
            globalScope.moment = deprecate(
                    'Accessing Moment through the global scope is ' +
                    'deprecated, and will be removed in an upcoming ' +
                    'release.',
                    moment);
        } else {
            globalScope.moment = moment;
        }
    }

    // CommonJS module is defined
    if (hasModule) {
        module.exports = moment;
    } else if (typeof define === 'function' && define.amd) {
        define('moment', function (require, exports, module) {
            if (module.config && module.config() && module.config().noGlobal === true) {
                // release the global variable
                globalScope.moment = oldGlobalMoment;
            }

            return moment;
        });
        makeGlobal(true);
    } else {
        makeGlobal();
    }
}).call(this);

}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{}],12:[function(require,module,exports){

/**
 * Module dependencies.
 */

var global = (function() { return this; })();

/**
 * WebSocket constructor.
 */

var WebSocket = global.WebSocket || global.MozWebSocket;

/**
 * Module exports.
 */

module.exports = WebSocket ? ws : null;

/**
 * WebSocket constructor.
 *
 * The third `opts` options object gets ignored in web browsers, since it's
 * non-standard, and throws a TypeError if passed to the constructor.
 * See: https://github.com/einaros/ws/issues/227
 *
 * @param {String} uri
 * @param {Array} protocols (optional)
 * @param {Object) opts (optional)
 * @api public
 */

function ws(uri, protocols, opts) {
  var instance;
  if (protocols) {
    instance = new WebSocket(uri, protocols);
  } else {
    instance = new WebSocket(uri);
  }
  return instance;
}

if (WebSocket) ws.prototype = WebSocket.prototype;

},{}],13:[function(require,module,exports){
var WisebedPublicReservationData = function(prd) {
	
	this.from = moment(prd.from);
	this.to = moment(prd.to);
	this.cancelled = prd.cancelled ? moment(prd.cancelled) : undefined;
	this.finalized = prd.finalized ? moment(prd.finalized) : undefined;
	this.nodeUrns = prd.nodeUrns;
	this.nodeUrnPrefixes = [];
	
	var self = this;
	
	prd.nodeUrns.forEach(function(nodeUrn) {
		var nodeUrnPrefix = nodeUrn.substring(0, nodeUrn.lastIndexOf(':') + 1);
		if (self.nodeUrnPrefixes.indexOf(nodeUrnPrefix) < 0) {
			self.nodeUrnPrefixes.push(nodeUrnPrefix);
		}
	});

	this.nodeUrns.sort();
	this.nodeUrnPrefixes.sort();
};

var WisebedConfidentialReservationData = function(crd) {
	
	this.description = crd.description;
	this.from = moment(crd.from);
	this.to = moment(crd.to);
	this.cancelled = crd.cancelled ? moment(crd.cancelled) : undefined;
	this.finalized = crd.finalized ? moment(crd.finalized) : undefined;
	this.nodeUrns = crd.nodeUrns;
	this.nodeUrnPrefixes = [];
	this.options = crd.options;
	this.secretReservationKey = crd.secretReservationKey;
	this.username = crd.username;
	
	var self = this;

	crd.nodeUrns.forEach(function(nodeUrn) {
		var nodeUrnPrefix = nodeUrn.substring(0, nodeUrn.lastIndexOf(':') + 1);
		if (self.nodeUrnPrefixes.indexOf(nodeUrnPrefix) < 0) {
			self.nodeUrnPrefixes.push(nodeUrnPrefix);
		}
	});

	this.nodeUrns.sort();
	this.nodeUrnPrefixes.sort();
	this.serializedSecretReservationKeyBase64 = btoa(JSON.stringify([this.secretReservationKey]));
	this.experimentId = this.serializedSecretReservationKeyBase64;
};

var WisebedReservation = function(confidentialReservationDataList) {

	this.descriptions = [];
	this.description = '';
	this.from = null;
	this.to = null;
	this.cancelled = null;
	this.finalized = null;
	this.nodeUrns = [];
	this.nodeUrnPrefixes = [];
	this.confidentialReservationDataList = [];
	this.secretReservationKeys = [];
	this.serializedSecretReservationKeyBase64 = null;
	this.experimentId = null;

	var self = this;

	confidentialReservationDataList.forEach(function(confidentialReservationData) {
		var crd = new WisebedConfidentialReservationData(confidentialReservationData);
		if (crd.description && crd.description !== '') {
			self.descriptions.push(crd.description);
		}
		if (self.from === null || crd.from >= self.from) {
			self.from = crd.from;
		}
		if (self.to === null || crd.to <= self.to  ) {
			self.to = crd.to;
		}
		if (self.cancelled === null || crd.cancelled <= self.cancelled) {
			self.cancelled = crd.cancelled;
		}
		if (self.finalized === null || crd.finalized <= self.finalized) {
			self.finalized = crd.finalized;
		}
		crd.nodeUrns.forEach(function(nodeUrn) {
			self.nodeUrns.push(nodeUrn);
			var nodeUrnPrefix = nodeUrn.substring(0, nodeUrn.lastIndexOf(':') + 1);
			if (self.nodeUrnPrefixes.indexOf(nodeUrnPrefix) < 0) {
				self.nodeUrnPrefixes.push(nodeUrnPrefix);
			}
		});
		self.secretReservationKeys.push(crd.secretReservationKey);
		self.confidentialReservationDataList.push(new WisebedConfidentialReservationData(crd));
	});
	this.nodeUrns.sort();
	this.nodeUrnPrefixes.sort();
	this.secretReservationKeys.sort(function(a,b) {
		if (a.nodeUrnPrefix < b.nodeUrnPrefix) { return -1; }
		if (a.nodeUrnPrefix > b.nodeUrnPrefix) { return  1; }
		if (a.key < b.key) { return -1; }
		if (a.key > b.key) { return  1; }
		return 0;
	});
	this.serializedSecretReservationKeyBase64 = btoa(JSON.stringify(this.secretReservationKeys));
	this.experimentId = this.serializedSecretReservationKeyBase64;
	this.description = this.descriptions.join('<br/>');
};

var Wisebed = function(baseUri, webSocketBaseUri) {

	function addAuthHeader(credentials) {
		return function(xhr) {
			xhr.setRequestHeader("X-WISEBED-Authentication-Triple", JSON.stringify({ authenticationData : credentials }));
		};
	}

	function getBaseUri() {
		return baseUri;
	}

	function getWebSocketBaseUri() {
		return webSocketBaseUri;
	}

	return {

		EventWebSocket : function(onDevicesAttached, onDevicesDetached, onOpen, onClose) {

			this.onDevicesAttached = onDevicesAttached;
			this.onDevicesDetached = onDevicesDetached;
			this.onOpen = onOpen;
			this.onClose = onClose;

			var self = this;
			this.socket = new WebSocket(getWebSocketBaseUri() + '/events');
			this.socket.onmessage = function(evt) {
				var event = JSON.parse(evt.data);
				if (event.type == 'keepAlive') {
					// ignore
				} else if (event.type == 'devicesAttached') {
					self.onDevicesAttached(event);
				} else if (event.type == 'devicesDetached') {
					self.onDevicesDetached(event);
				} else {
					console.log("Received unknown event over event bus: " + event);
				}
			};
			this.socket.onopen  = function(event) { self.onOpen(event);  };
			this.socket.onclose = function(event) { self.onClose(event); };
		},

		WebSocket : function(experimentId, onmessage, onopen, onclosed) {

			this.experimentId = experimentId;
			this.onmessage    = onmessage;
			this.onopen       = onopen;
			this.onclosed     = onclosed;

			var self = this;
			var url = getWebSocketBaseUri() + '/experiments/' + encodeURIComponent(this.experimentId);

			this.socket = new WebSocket(url);
			this.socket.onmessage = function(event) { self.onmessage(JSON.parse(event.data)); };
			this.socket.onopen    = function(event) { self.onopen(event); };
			this.socket.onclose   = function(event) { self.onclosed(event); };

			this.send = function(message) {
				this.socket.send(JSON.stringify(message, null, '  '));
			};

			this.close = function(code, reason) {
				this.socket.close(code !== undefined ? code : 1000, reason !== undefined ? reason : '');
			};
		},

		testCookie : function (callbackOK, callbackError) {

			// Check cookie
			var getCookieCallbackDone = function() {
				$.ajax({
					url       : getBaseUri() + "/cookies/check",
					success   : callbackOK,
					error     : callbackError,
					xhrFields : { withCredentials: true }
				});
			};

			// Get cookie
			$.ajax({
				url       : getBaseUri() + "/cookies/get",
				success   : getCookieCallbackDone,
				error     : callbackError,
				xhrFields : { withCredentials: true }
			});
		},

		reservations : {

			/**
			 * returns a WisebedReservation for the given experimentId (serialized base64-encoded secret reservation key(s))
			 */
			getByExperimentId : function(experimentId, callbackDone, callbackError) {
				var queryUrl = getBaseUri() + "/reservations/byExperimentId/" + encodeURIComponent(experimentId);
				$.ajax({
					url       : queryUrl,
					success   : function(confidentialReservationDataList, textStatus, jqXHR) {
						callbackDone(new WisebedReservation(confidentialReservationDataList), textStatus, jqXHR);
					},
					error     : callbackError,
					dataType  : "json",
					xhrFields : { withCredentials: true }
				});
			},

			/**
			 * returns a list of WisebedReservation objects
			 */
			getPersonal : function(from, to, callbackDone, callbackError, credentials, showCancelled) {
				var queryUrl = getBaseUri() + "/reservations/personal?" +
						(from ? ("from=" + from.toISOString() + "&") : "") +
						(to ? ("to="+to.toISOString() + "&") : "") +
						((showCancelled !== undefined) ? ("showCancelled="+ showCancelled + "&") : "");
				$.ajax({
					url        : queryUrl,
					success    : function(crdList, textStatus, jqXHR) {
						var list = [];
						crdList.forEach(function(crd) { list.push(new WisebedReservation([crd])); });
						callbackDone(list, textStatus, jqXHR);
					},
					error      : callbackError,
					beforeSend : credentials ? addAuthHeader(credentials) : undefined,
					dataType   : "json",
					xhrFields  : { withCredentials: true }
				});
			},

			/**
			 * returns a list of WisebedPublicReservationData objects
			 */
			getPublic : function(from, to, callbackDone, callbackError, showCancelled) {
				var queryUrl = getBaseUri() + "/reservations/public?" +
						(from ? ("from=" + from.toISOString() + "&") : "") +
						(to ? ("to=" + to.toISOString() + "&") : "") +
						((showCancelled !== undefined) ? ("showCancelled=" + showCancelled + "&") : "");
				$.ajax({
					url       : queryUrl,
					success   : function(prdList, textStatus, jqXHR) {
						var list = [];
						prdList.forEach(function(prd) { list.push(new WisebedPublicReservationData(prd)); });
						callbackDone(list, textStatus, jqXHR);
					},
					error     : callbackError,
					dataType  : "json",
					xhrFields : { withCredentials: true }
				});
			},

			getFederatable : function(from, to, callbackDone, callbackError) {

				function calculatePowerset(ary) {
					var ps = [[]];
					for (var i=0; i < ary.length; i++) {
						for (var j = 0, len = ps.length; j < len; j++) {
							ps.push(ps[j].concat(ary[i]));
						}
					}
					return ps;
				}

				this.getPersonal(from, to, function(reservations) {

					var powerset = calculatePowerset(reservations);
					var federatableSets = [];
					var current, currentRes, overlap;

					for (var i=0; i<powerset.length; i++) {
						
						if (powerset[i].length === 0) {continue;} // first element (empty set) doesn't make sense
						if (powerset[i].length === 1) {continue;} // single reservation sets can't be federated

						// for every reservation in the current set of reservations check if reservation interval
						// overlaps with reservation interval of each other reservation in the set
						current = powerset[i];
						overlap = true;
						
						for (var k=0; k<current.length; k++) {
							for (var l=k; l<current.length; l++) {

								// reservations overlap if (startA <= endB) and (endA >= startB)
								if (!(current[k].from < current[l].to && current[k].to > current[l].from)) {
									overlap = false;
								}
							}
						}

						if (overlap) {
							federatableSets.push(current);
						}
					}

					var federatableReservations = [];
					federatableSets.forEach(function(federatableSet) {
						var federatableSetCrds = [];
						federatableSet.forEach(function(federatable) {
							federatableSetCrds = federatableSetCrds.concat(federatable.confidentialReservationDataList);
						});
						federatableReservations.push(new WisebedReservation(federatableSetCrds));
					});

					callbackDone(federatableReservations);

				}, callbackError);
			},

			/**
			 * returns a WisebedReservation object
			 */
			make : function(from, to, nodeUrns, description, options, callbackDone, callbackError, credentials) {

				// Generate JavaScript object
				var content = {
					"from"        : from.toISOString(),
					"nodeUrns"    : nodeUrns,
					"to"          : to.toISOString(),
					"description" : description,
					"options"     : options
				};

				$.ajax({
					url			:	getBaseUri() + "/reservations/create",
					type		:	"POST",
					data		:	JSON.stringify(content, null, '  '),
					contentType	:	"application/json; charset=utf-8",
					dataType	:	"json",
					success		: 	function(confidentialReservationDataList, textStatus, jqXHR) {
						callbackDone(new WisebedReservation(confidentialReservationDataList), textStatus, jqXHR);
					},
					beforeSend  : 	credentials ? addAuthHeader(credentials) : undefined,
					error		: 	callbackError,
					xhrFields   : { withCredentials: true }
				});

			},

	        cancel : function(experimentId, callbackDone, callbackError) {
				var queryUrl = getBaseUri() + "/reservations/byExperimentId/" + encodeURIComponent(experimentId);
				$.ajax({
	                type      : 'DELETE',
					url       : queryUrl,
					success   : callbackDone,
					error     : callbackError,
					context   : document.body,
					dataType  : "json",
					xhrFields : { withCredentials: true }
				});
			},

			equals : function(res1, res2) {

				function subsetOf(set1, set2, compare) {
					for (var i=0; i<set1.length; i++) {
						for (var j=0; j<set2.length; j++) {
							if (!compare(set1[i], set2[j])) {
								return false;
							}
						}
					}
					return true;
				}

				function setEquals(set1, set2, compare) {

					if (set1.length != set2.length) {
						return false;
					}

					return subsetOf(set1, set2, compare) && subsetOf(set2, set1, compare);
				}

				return setEquals(res1, res2, function(dataElem1, dataElem2) {
					return  dataElem1.secretReservationKey == dataElem2.secretReservationKey &&
							dataElem1.urnPrefix            == dataElem2.urnPrefix;
				});
			}
		},

		experiments : {

			getConfiguration : function (url, callbackDone, callbackError) {
				$.ajax({
					url       : getBaseUri() + "/experimentconfiguration",
					type      : "GET",
					data      : {url: url},
					success   : callbackDone,
					error     : callbackError,
					dataType  : "json",
					xhrFields : { withCredentials: true }
				});
			},

			areNodesConnected : function(nodeUrns, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/areNodesConnected",
					type        : "POST",
					data        : JSON.stringify({nodeUrns:nodeUrns}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data.operationStatus);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			areNodesAlive : function(experimentId, nodeUrns, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/areNodesAlive",
					type        : "POST",
					data        : JSON.stringify({nodeUrns:nodeUrns}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data.operationStatus);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			send : function(experimentId, nodeUrns, messageBytesBase64, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/send",
					type        : "POST",
					data        : JSON.stringify({
						sourceNodeUrn  : 'user',
						targetNodeUrns : nodeUrns,
						bytesBase64    : messageBytesBase64
					}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data.operationStatus);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			resetNodes : function(experimentId, nodeUrns, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/resetNodes",
					type        : "POST",
					data        : JSON.stringify({nodeUrns:nodeUrns}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data.operationStatus);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			getNodeUrns : function(experimentId, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/nodeUrns",
					type        : "GET",
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data.nodeUrns);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			getChannelPipelines : function(experimentId, nodeUrns, callbackDone, callbackError) {

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/getChannelPipelines",
					type        : "POST",
					data        : JSON.stringify({nodeUrns:nodeUrns}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			setChannelPipelines : function(experimentId, nodeUrns, handlers, callbackDone, callbackError) {
				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/setChannelPipelines",
					type        : "POST",
					data        : JSON.stringify({
						nodeUrns : nodeUrns,
						handlers : handlers
					}, null, '  '),
					contentType : "application/json; charset=utf-8",
					dataType    : "json",
					success     : function(data) {callbackDone(data);},
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			},

			flashNodes : function(experimentId, data, callbackDone, callbackProgress, callbackError) {

				function getAllNodeUrnsFromRequestData(data) {

					var allNodeUrns = [];

					for (var i=0; i<data.configurations.length; i++) {
						var configuration = data.configurations[i];
						for (var j=0; j<configuration.nodeUrns.length; j++) {
							allNodeUrns.push(configuration.nodeUrns[j]);
						}
					}

					allNodeUrns.sort();
					return allNodeUrns;
				}

				var allNodeUrns = getAllNodeUrnsFromRequestData(data);

				var requestSuccessCallback = function(d, textStatus, jqXHR){

					// Headers are empty in Cross-Site-Environment
					//var flashRequestStatusURL = jqXHR.getResponseHeader("Location");
					var flashRequestStatusURL = jqXHR.responseText;

					var compareArrays = function(arr1, arr2) {
						
						if (arr1.length != arr2.length) {
							return false;
						}

						for (var i = 0; i < arr1.length; i++) {
							if (arr1[i] != arr2[i]) {
								return false;
							}
						}

						return true;
					};

					var schedule = setInterval(function() {

						var onProgressRequestSuccess = function(data) {

							//var data = JSON.parse(d);
							var completeNodeUrns = [];

							$.each(data.operationStatus, function(nodeUrn, nodeStatus) {
								if (nodeStatus.status != 'RUNNING') {
									completeNodeUrns.push(nodeUrn);
								}
							});

							completeNodeUrns.sort();

							if (compareArrays(allNodeUrns, completeNodeUrns)) {
								callbackDone(data.operationStatus);
								clearInterval(schedule);
							} else {
								callbackProgress(data.operationStatus);
							}
						};

						var onProgressRequestError = function(jqXHR, textStatus, errorThrown) {
							clearInterval(schedule);
							callbackError(jqXHR, textStatus, errorThrown);
						};

						$.ajax({
							url         : flashRequestStatusURL,
							type        : "GET",
							success     : onProgressRequestSuccess,
							error       : onProgressRequestError,
							dataType    : "json",
							xhrFields   : { withCredentials: true }
						});

					}, 2 * 1000);
				};

				$.ajax({
					url         : getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/flash",
					type        : "POST",
					data        : JSON.stringify(data, null, '  '),
					contentType : "application/json; charset=utf-8",
					success     : requestSuccessCallback,
					error       : callbackError,
					xhrFields   : { withCredentials: true }
				});
			}
		},

		getNodeUrnArray : function(experimentId, callbackDone, callbackError) {

			this.getWiseML(
					experimentId,
					function(wiseML, textStatus, jqXHR) {
						callbackDone(this.getNodeUrnArrayFromWiseML(wiseML), textStatus, jqXHR);
					},
					callbackError
			);
		},

		getWiseML : function(experimentId, callbackDone, callbackError, jsonOrXml, callbackComplete) {

			var dataType = (!jsonOrXml ? "json" : jsonOrXml);
			var url = (experimentId ?
						getBaseUri() + "/experiments/" + encodeURIComponent(experimentId) + "/network." + dataType :
						getBaseUri() + "/experiments/network." + dataType);

			$.ajax({
				url      : url,
				cache    : false,
				success  : callbackDone,
				error    : callbackError,
				complete : callbackComplete,
				accepts  : dataType,
				dataType : dataType == 'xml' ? 'text' : dataType, // workaround because jQuery fails to parse this valid XML
				xhrFields: { withCredentials: true }
			});
		},

		getWiseMLAsJSON : function(experimentId, callbackDone, callbackError, callbackComplete) {
			this.getWiseML(experimentId, callbackDone, callbackError, "json", callbackComplete);
		},

		getWiseMLAsXML : function(experimentId, callbackDone, callbackError, callbackComplete) {
			this.getWiseML(experimentId, callbackDone, callbackError, "xml", callbackComplete);
		},

		getNodeUrnArrayFromWiseML : function(wiseML) {
			var nodeUrns = [];
			var nodes = wiseML.setup.node;
			for (var i=0; i<nodes.length; i++) {
				nodeUrns[i] = nodes[i].id;
			}
			return nodeUrns;
		},

		getTestbedDescription : function(callbackDone, callbackError) {
			$.ajax({
				url       : getBaseUri() + "/testbedDescription",
				success   : callbackDone,
				error     : callbackError,
				dataType  : "json",
				xhrFields : { withCredentials: true }
			});
		},

		hasSecretAuthenticationKeyCookie : function() {
			return $.cookie('wisebed-secret-authentication-key') !== null;
		},

		isLoggedIn : function(callbackDone, callbackError) {
			$.ajax({
				url      : getBaseUri() + "/auth/isLoggedIn",
				dataType : "json",
				success  : function() {callbackDone(true);},
				error    : function(jqXHR, textStatus, errorThrown) {
					if (jqXHR.status == 403) {
						callbackDone(false);
					} else {
						callbackError(jqXHR, textStatus, errorThrown);
					}
				},
				xhrFields: { withCredentials: true }
			});
		},

		login : function(credentials, callbackDone, callbackError) {
			$.ajax({
				url			: getBaseUri() + "/auth/login",
				type		: "POST",
				data		: JSON.stringify(credentials, null, '  '),
				contentType	: "application/json; charset=utf-8",
				dataType	: "json",
				error		: callbackError,
				success		: callbackDone,
				xhrFields   : { withCredentials: true }
			});
		},

		logout : function(callbackDone, callbackError) {
			$.ajax({
				url       : getBaseUri() + "/auth/logout",
				success   : callbackDone,
				error     : callbackError,
				xhrFields : { withCredentials: true }
			});
		}
	};
};

var $      = require('jquery');
var moment = require('moment');
var btoa   = require('btoa');
var atob   = require('atob');

var WebSocket = typeof window == 'undefined' ? require('ws') : window.MozWebSocket || window.WebSocket;
// if running in a browser use the window.WebSocket object
// if running in node use "ws" library

module.exports = {
  Wisebed                            : Wisebed,
  WisebedPublicReservationData       : WisebedPublicReservationData,
  WisebedConfidentialReservationData : WisebedConfidentialReservationData,
  WisebedReservation                 : WisebedReservation
};

},{"atob":8,"btoa":9,"jquery":10,"moment":11,"ws":12}],14:[function(require,module,exports){
var StringUtils = function() {
	
	var startsWith = function (str, patt) {
		return str.slice(0, patt.length) == patt;
	};
	
	var replacements = [];
	replacements[0x00] = "[NUL]";
	replacements[0x01] = "[SOH]";
	replacements[0x02] = "[STX]";
	replacements[0x03] = "[ETX]";
	replacements[0x04] = "[EOT]";
	replacements[0x05] = "[ENQ]";
	replacements[0x06] = "[ACK]";
	replacements[0x07] = "[BEL]";
	replacements[0x08] = "[BS]";
	replacements[0x09] = "[TAB]";
	replacements[0x0a] = "[LF]";
	replacements[0x0b] = "[VT]";
	replacements[0x0c] = "[FF]";
	replacements[0x0d] = "[CR]";
	replacements[0x0e] = "[SO]";
	replacements[0x0f] = "[SI]";
	replacements[0x10] = "[DLE]";
	replacements[0x11] = "[DC1]";
	replacements[0x12] = "[DC2]";
	replacements[0x13] = "[DC3]";
	replacements[0x14] = "[DC4]";
	replacements[0x15] = "[NACK]";
	replacements[0x16] = "[SYN]";
	replacements[0x17] = "[ETB]";
	replacements[0x18] = "[CAN]";
	replacements[0x19] = "[EM]";
	replacements[0x1a] = "[SUB]";
	replacements[0x1b] = "[ESC]";
	replacements[0x1c] = "[FS]";
	replacements[0x1d] = "[GS]";
	replacements[0x1e] = "[RS]";
	replacements[0x1f] = "[US]";
	for (var i = 0x20; i < 0x7f; i++) {
		replacements[i] = String.fromCharCode(i & 0xFF);
	}
	replacements[0x7f] = "[DEL]";
	for (i = 128; i < 256; i++) {
		var hex = (i).toString(16);
		var paddedHex = ("00" + hex).substring(hex.length);
		replacements[i] = "[0x" + paddedHex + "]";
	}
	this.r = replacements;
	/**
	 * Construct an integer from an input string Supported Prefixes are "0x" (hexadecimal) and "0b" (binary), otherwise base 10
	 * (decimal) is assumed. Examples: 0x0A 0x1B 0b11001001 40 40 0b11001001 0x1F
	 **/
	this.string2int = function(input) {
		var radix = 10;

		if (startsWith(input,'0x')) {
			radix = 16;
			input = input.replace("0x", "");

		} else if (startsWith(input,'0b')) {
			radix = 2;
			input = input.replace("0b", "");
		}
		return parseInt(input, radix);
	};
	
	this.makePrintable = function(arr) {
		var res = '';
		for (var i=0; i<arr.length; i++) {
			res += replacements[arr[i].charCodeAt(0)];
		}
		return res;
	};
	
	var padl = function(str, pad, num) {
		return (Array(num+1).join(pad) + str).substring(str.length);
	};
	
	var toXArray = function(str, radix) {
		var res = [];
		for (var i=0; i< str.length; i++) {
			var num = (str.charCodeAt(i)).toString(radix);
			switch (radix) {
				case 16:
					num = padl(num,'0',2);
					break;
				case 10:
					num = padl(num,'0',3);
					break;
				case 2:
					num = padl(num,'0',8);
					break;
			}
			res.push(num);
		}
		return res;
	};
	
	this.toHexArray = function(str) {
		return toXArray(str, 16);
	};
	
	this.toDecimalArray = function(str) {
		return toXArray(str, 10);
	};
	
	this.toBinaryArray = function(str) {
		return toXArray(str, 2);
	};
	
	this.toHexString = function(str) {
		return toXstring(str, 16).join(' ');
	};
	
	this.toDecimalString = function(str) {
		return toXstring(str, 10).join(' ');
	};
	
	this.toBinaryString = function(str) {
		return toXstring(str, 2).join(' ');
	};
};

module.exports = StringUtils;
},{}],15:[function(require,module,exports){
var WiseGuiEvents              = require('./wisegui-events.js');
var WiseGuiNodeSelectionDialog = require('./wisegui-nodeselection-dialog.js');
var StringUtils                = require('./string-utils.js');

/**
 * #################################################################
 * WiseGuiConsoleView
 * #################################################################
 */

var WiseGuiConsoleView = function(reservation) {

	var id = reservation.experimentId.replace(/=/g, '');

	this.reservation                  = reservation;
	this.experimentId                 = reservation.experimentId;

	this.columns                      = { 'time' : true, 'urn' : true, 'message' : true };
	this.outputsNumMessages           = 100;
	this.outputsRedrawLimit           = 200;
	this.outputs                      = [];
	this.outputsFollow                = true;
	this.outputsMakePrintable         = true;
	this.outputsType                  = 'ascii';
	this.socket                       = null;
	this.view                         = null;

	this.progressBarId                = 'WiseGuiConsoleView-progress-bar-'+id;
	this.progressBar                  = null;
	this.progressBarSchedule          = undefined;
	this.statusBadgeId                = 'WiseGuiConsoleView-status-badge-'+id;
	this.statusBadgeDetachedId        = 'WiseGuiConsoleView-status-badge-detached-'+id;
	this.statusBadge                  = null;
	this.outputsNumMessagesInput      = null;
	this.outputsRedrawLimitInput      = null;
	this.outputsTable                 = null;
	this.outputsClearButton           = null;
	this.outputsFollowCheckbox        = null;
	this.outputsFilterButton          = null;
	this.outputsColumnDropdown        = null;
	this.outputsViewDropdown          = null;
	this.outputsMakePrintableCheckbox = null;

	this.buildView();
	this.connectToReservation();

	var self = this;
	this.throttledRedraw = $.throttle(self.outputsRedrawLimit, function(){ self.redrawOutput(); });
};

WiseGuiConsoleView.prototype.buildView = function() {
	this.view = $(
			'<div class="WiseGuiConsoleViewDiv">' +
			'	<div class="row">' +
			'		<div class="span5">' +
			'			<span><b>start:</b> ' + this.reservation.from.format("YYYY-MM-DD HH:mm:ss") + '</b></span>' +
			'			<span class="pull-right"><b>end:</b> ' + this.reservation.to.format("YYYY-MM-DD HH:mm:ss") + '</span>' +
			'			<div id="' + this.progressBarId+ '" class="progress"><div class="bar" style="width: 0%;"></div></div>' +
			'		</div>' +
			'		<div class="span3">' +
			'			<span id="' + this.statusBadgeId + '" class="badge">Loading...</span>' +
			'			<span id="' + this.statusBadgeDetachedId + '" class="badge badge-important" style="display: none;">Loading...</span>' +
			'		</div>' +
			'		<div class="span4">' +
			'			<div class="btn-toolbar btn-toolbar2 pull-right">' +
			'				<div class="btn-group">' +
			'					<button id="download-log" class="btn" title="Download Log"><i class="icon-download"></i></button>' +
			'				</div>' +
			'			</div>' +
			'			<div class="btn-toolbar btn-toolbar2 pull-right">' +
			'				<div class="btn-group">' +
			'					<button id="pause-output" class="btn" data-toggle="button" title="Pause receiving messages"><i class="icon-pause"></i></button>' +
			'				</div>' +
			'				<div class="btn-group">' +
			'					<button id="clear-output" class="btn" title="Clear output"><i class="icon-remove"></i></button>' +
			'				</div>' +
			'				<div class="btn-group">' +
			'					<button id="output-view" data-toggle="dropdown" class="btn dropdown-toggle" title="Change shown columns">' +
			'						<i class="icon-th"></i>&nbsp;<span class="caret"</span>' +
			'					</button>' +
			'					<ul id="column-dropdown" class="dropdown-menu">' +
			'						<li><strong>Show Columns:</strong></li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="checkbox" data-col="time" checked>' +
			'									Timestamp' +
			'							</label>' +
	    	'						</li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="checkbox" data-col="urn" checked>' +
			'									Node URN' +
			'							</label>' +
	    	'						</li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="checkbox" data-col="message" checked>' +
			'									Message' +
			'							</label>' +
			'						</li>' +
			'					</ul>' +
			'				</div>' +
			'				<div class="btn-group">' +
			'					<button id="output-view" data-toggle="dropdown" class="btn dropdown-toggle" title="Change view options">' +
			'						<i class="icon-eye-open"></i>&nbsp;<span class="caret"</span>' +
			'					</button>' +
			'					<ul class="dropdown-menu" id="view-dropdown">' +
			'						<li><strong>Format ouput as:</strong></li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="radio" name="viewRadio" id="optionsRadios1" value="ascii" checked>' +
			'									ASCII' +
			'							</label>' +
			'						</li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="radio" name="viewRadio" id="optionsRadios1" value="hex">' +
			'									Hex' +
			'							</label>' +
			'						</li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="radio" name="viewRadio" id="optionsRadios1" value="decimal">' +
			'									Decimal' +
			'							</label>' +
			'						</li>' +
			'						<li>' +
			'							<label class="radio">' +
			'								<input type="radio" name="viewRadio" id="optionsRadios1" value="binary">' +
			'									Binary' +
			'							</label>' +
			'						</li>' +
			'						<li>' +
			'							<label class="checkbox" title="Show non-printable chars in ASCII mode: e.g. 0x00 gets [NUL]">' +
			'								<input id="make-printable" checked type="checkbox">non-printables</input>' +
			'							</label>' +
			'						</li>' +
			'					</ul>' +
			'				</div>' +
			'				<div class="btn-group">' +
			'					<button id="filter-nodes" class="btn" title="Filter output for nodes."><i class="icon-filter"></i></button>' +
			'				</div>' +
			'				<div class="btn-group">' +
			'					<button class="btn dropdown-toggle" data-toggle="dropdown" title="Show output options.">' +
			'						<i class="icon-wrench"></i>&nbsp;<span class="caret"</span>' +
			'					</button>' +
			'					<ul id="options-dropdown" class="dropdown-menu">' +
			'						<li>' +
			'							<form class="form-inline">' +
			'								<label class="checkbox">' +
			'									<input id="auto-scroll" checked="checked" type="checkbox">auto scroll</input>' +
			'								</label>' +
			'							</form>' +
			'						</li>' +
			'						<li>' +
			'							<form class="form-inline">' +
			'								<label title="press enter to save">' +
			'									Show <input type="text" value="'+this.outputsNumMessages+'" id="num-outputs" style="width: 30px; height: 10px;"> messages' +
			'								</label>' +
			'							</form>' +
			'						</li>' +
			'						<li>' +
			'							<form class="form-inline">' +
			'								<label title="Incresing helps if your browser freezes due to too many messages. Press enter to save.">' +
			'									Redraw at most every <input type="text" value="'+this.outputsRedrawLimit+'" id="redraw-limit" style="width: 30px; height: 10px;"> ms' +
			'								</label>' +
			'							</form>' +
			'						</li>' +
			'					</ul>' +
			'				</div>' +
			'			</div>' +
			'		</div>' +
			'	</div>' +
			'	<div class="row">' +
			'		<div class="span12"><div class="well WiseGuiConsoleViewOutputsWell" style="height:300px; overflow:auto;">' +
			'			<table class="table WiseGuiConsoleViewOutputsTable">' +
			'				<tbody></tbody>' +
			'			</table>' +
			'		</div></div>' +
			'	</div>');

	var self = this;
	this.progressBar                  = this.view.find('#'+this.progressBarId).first();
	this.progressBarSchedule          = undefined;
	this.statusBadge                  = this.view.find('#'+this.statusBadgeId).first();
	this.statusBadgeDetached          = this.view.find('#'+this.statusBadgeDetachedId).first();
	this.outputsNumMessagesInput      = this.view.find('#num-outputs').first();
	this.outputsRedrawLimitInput      = this.view.find('#redraw-limit').first();
	this.outputsTable                 = this.view.find('table.WiseGuiConsoleViewOutputsTable tbody').first();
	this.outputsClearButton           = this.view.find('.btn#clear-output').first();
	this.outputsFollowCheckbox        = this.view.find('#auto-scroll').first();
	this.outputsFilterButton          = this.view.find('.btn#filter-nodes').first();
	this.outputsColumnDropdown        = this.view.find('#column-dropdown');
	this.outputsViewDropdown          = this.view.find('#view-dropdown');
	this.outputsMakePrintableCheckbox = this.view.find('#make-printable');
	this.outputsDownloadButton        = this.view.find('.btn#download-log');

	WiseGui.bindToReservationState(this.view.find('button:not(#download-log)'), this.experimentId);

	var now = moment();
	var interval;
	var statusBadgeInterval;
	var status = 'pending';
	var devicesDetached = [];

	var updateStatusBadge = function() {
		
		self.statusBadge.empty();

		if (status == 'pending') {

			self.statusBadge.addClass('badge-info');
			self.statusBadge.append('Reservation starting in ' + self.reservation.from.fromNow());

		} else if (status == 'running') {
			
			self.statusBadge.removeClass('badge-info').addClass('badge-success');
			self.statusBadge.append('Reservation running since ' + self.reservation.from.fromNow());

			if (devicesDetached.length > 0) {
				
				self.statusBadgeDetached.empty();
				self.statusBadgeDetached.append(devicesDetached.length + ' devices currently detached');
				self.statusBadgeDetached.toggle(true);

				self.statusBadgeDetached.tooltip('hide');
				self.statusBadgeDetached.data('tooltip', false);
				self.statusBadgeDetached.tooltip({title: devicesDetached.join('<br/>'), placement:'bottom' });

			} else if (self.statusBadgeDetached !== null) {

				self.statusBadgeDetached.empty();
				self.statusBadgeDetached.tooltip('hide');
				self.statusBadgeDetached.data('tooltip', false);
				self.statusBadgeDetached.toggle(false);
			}

		} else if (status == 'cancelled') {

			self.statusBadge.removeClass('badge-info badge-success').addClass('badge-warning');
			self.statusBadge.append('Reservation cancelled ' + self.reservation.cancelled.fromNow());

			self.statusBadgeDetached.empty();
			self.statusBadgeDetached.tooltip('hide');
			self.statusBadgeDetached.data('tooltip', false);
			self.statusBadgeDetached.toggle(false);

		} else if (status == 'ended') {

			self.statusBadge.removeClass('badge-info badge-success').addClass('badge-warning');
			self.statusBadge.append('Reservation ended ' + self.reservation.to.fromNow());
			
			self.statusBadgeDetached.empty();
			self.statusBadgeDetached.tooltip('hide');
			self.statusBadgeDetached.data('tooltip', false);
			self.statusBadgeDetached.toggle(false);
		}
	};

	updateStatusBadge();
	statusBadgeInterval = window.setInterval(updateStatusBadge, 5000);
	
	$(window).bind(WiseGuiEvents.EVENT_RESERVATION_STARTED, function(e, reservation) {
		if (self.experimentId == reservation.experimentId) {
			
			self.reservation = reservation;

			// start progress bar as soon as reservation starts
			self.progressBar.toggleClass('progress-success', true);
			self.progressBar.find('div.bar').css('width', '1%');
			self.progressBarSchedule = window.setInterval(function(){
				var durationInMillis = reservation.to.unix() - reservation.from.unix();
				var millisSinceStart = moment().unix() - reservation.from.unix();
				var passedInPercent  = (millisSinceStart / durationInMillis) * 100;
				self.progressBar.find('div.bar').css('width', (passedInPercent > 1 ? passedInPercent + '%' : '1%'));
			}, 1000);

			// update status badge to show device connectivity
			status = 'running';
			updateStatusBadge();
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_RESERVATION_CANCELLED, function(e, reservation) {
		if (self.experimentId == reservation.experimentId) {

			self.reservation = reservation;

			// stop progress bar as soon as reservation has just been cancelled
			window.clearInterval(self.progressBarSchedule);
			self.progressBar.toggleClass('progress-success', false);
			self.progressBar.toggleClass('progress-warning', true);
			self.progressBar.find('div.bar').css('width', '100%');

			// update status badge
			status = 'cancelled';
			updateStatusBadge();
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_RESERVATION_ENDED, function(e, reservation) {
		if (self.experimentId == reservation.experimentId) {

			self.reservation = reservation;

			// stop progress bar as soon as reservation ends
			window.clearInterval(self.progressBarSchedule);
			self.progressBar.toggleClass('progress-success', false);
			self.progressBar.toggleClass('progress-warning', true);
			self.progressBar.find('div.bar').css('width', '100%');

			// update status badge
			status = 'ended';
			updateStatusBadge();
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_RESERVATION_FINALIZED, function(e, reservation) {
		if (self.experimentId == reservation.experimentId) {

			self.reservation = reservation;

			// stop progress bar as soon as reservation ends
			window.clearInterval(self.progressBarSchedule);
			self.progressBar.toggleClass('progress-success', false);
			self.progressBar.toggleClass('progress-warning', true);
			self.progressBar.find('div.bar').css('width', '100%');

			// update status badge
			status = 'ended';
			updateStatusBadge();
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_DEVICES_ATTACHED, function(e, devicesAttachedEvent) {

		devicesAttachedEvent.nodeUrns.forEach(function(nodeUrn) {
			if (self.reservation.nodeUrns.indexOf(nodeUrn) > -1) {
				var index = devicesDetached.indexOf(nodeUrn);
				if (index > -1) {
					devicesDetached.splice(index, 1);
				}
			}
		});

		updateStatusBadge();
	});

	$(window).bind(WiseGuiEvents.EVENT_DEVICES_DETACHED, function(e, devicesDetachedEvent) {

		devicesDetachedEvent.nodeUrns.forEach(function(nodeUrn) {
			if (self.reservation.nodeUrns.indexOf(nodeUrn) > -1) {
				var index = devicesDetached.indexOf(nodeUrn);
				if (index == -1) {
					devicesDetached.push(nodeUrn);
				}
			}
		});

		updateStatusBadge();
	});

	// don't close popup when clicking on a form
	this.view.find('.dropdown-menu').click(function(e) {
		e.stopPropagation();
	});

	// watch shown-columns-checkbox changes
	this.outputsColumnDropdown.find('input[type="checkbox"]').change(function() {
		self.columns[$(this).data('col')] = $(this).is(':checked');
		self.redrawOutput();
	});
	
	this.outputsViewDropdown.find('input').change(function(){
		self.redrawOutput();
	});

	this.outputsNumMessagesInput.bind('change', self, function(e) {
		var fieldValue = parseInt(self.outputsNumMessagesInput[0].value);
		if (isNaN(fieldValue)) {
			self.outputsNumMessagesInput.addClass('error');
		} else {
			self.outputsNumMessagesInput.removeClass('error');
			self.outputsNumMessages = fieldValue;
			self.redrawOutput();
		}
	});
	
	this.outputsRedrawLimitInput.bind('change', self, function(e) {
		var fieldValue = parseInt(self.outputsRedrawLimitInput[0].value);
		if (isNaN(fieldValue)) {
			self.outputsRedrawLimitInput.addClass('error');
		} else {
			self.outputsRedrawLimitInput.removeClass('error');
			self.outputsRedrawLimit = fieldValue;
			self.throttledRedraw = $.throttle(self.outputsRedrawLimit, function(){	self.redrawOutput();});
		}
	});

	this.outputsFollowCheckbox.bind('change', self, function(e) {
		self.outputsFollow = self.outputsFollowCheckbox.first().is(':checked');
	});
	
	this.outputsMakePrintableCheckbox.bind('change', self, function(e) {
		self.outputsMakePrintable = self.outputsMakePrintableCheckbox.is(':checked');
		self.redrawOutput();
	});

	this.outputsClearButton.bind('click', self, function(e) {
		self.outputs.length = 0;
		self.redrawOutput();
	});
	
	this.outputsViewDropdown.find('input[type="radio"]').bind('change', self, function(e) {
		self.outputsType = self.outputsViewDropdown.find('input[type="radio"]:checked"').attr('value');
		self.redrawOutput();
	});

	this.outputsFilterNodeSelectionDialog = new WiseGuiNodeSelectionDialog(
			self.experimentId,
			'Select Nodes',
			'Please select the nodes you want to see output from. If no nodes are selected, every output will be shown.',
			[],
			'nodeselection.output.'
	);
	this.outputsFilterButton.click(function(e) {
		self.outputsFilterButton.attr('disabled', true);
		self.outputsFilterNodeSelectionDialog.show(
			function() {
				self.outputsFilterButton.attr('disabled', false);
				self.updateOutputsFilterNodesButton();
				self.redrawOutput();
			},
			function() {
				self.outputsFilterButton.attr('disabled', false);
				self.updateOutputsFilterNodesButton();
			}
		);
	});

	this.outputsFilterNodeSelectionDialog.onReady(function() {self.updateOutputsFilterNodesButton(); });

	this.outputsDownloadButton.click(function(e) {
		var url = wisebedBaseUrl + '/events/' + self.reservation.experimentId + '.json';
		window.open(url, '_blank');
	});
};

WiseGuiConsoleView.prototype.redrawOutput = function() {

	// remove messages that are too much
	if (this.outputs.length > this.outputsNumMessages) {
		var elementsToRemove = this.outputs.length - this.outputsNumMessages;
		this.outputs.splice(0, elementsToRemove);
	}
	
	// 'draw' messages to table
	this.outputsTable.empty();
	var rows = null;
	for (var i=0; i<this.outputs.length; i++) {
		var row = this.generateRow(this.outputs[i]);
		rows = rows ? rows.after(row) : row;
	}

	this.outputsTable.append(rows);

	// scroll down if active
	if (this.outputsFollow) {
		var scrollArea = this.outputsTable.parent().parent();
		scrollArea.scrollTop(scrollArea[0].scrollHeight);
	}
};

WiseGuiConsoleView.prototype.generateRow = function(message) {

	var col = function(text) {
		return $('<td>').append(text);
	};
	var row = $('<tr></tr>');
	var cols = {
			'time' : message.timestamp,
			'urn' : message.sourceNodeUrn,
			'message' : this.formatBase64(message.payloadBase64)
	};

	var self = this;
	$.each(cols, function(key, val) {
		if (self.columns[key])
			row.append(col(val));
	});
	
	return row;
};

WiseGuiConsoleView.prototype.formatBase64 = function(base64) {
	var wrapEach = function(arr) {
		var ret = '';
		for (var i=0; i<arr.length; i++) {
			ret += '<span class="WiseGuiNumber">' + arr[i] + '</span>';
		}
		return ret;
	};
	var res = null;
	var msg = base64_decode(base64);
	switch (this.outputsType) {
		case 'ascii':
			if (this.outputsMakePrintable) {
				res = StringUtils.makePrintable(msg);
			} else {
				res = msg;
			}
			break;
		case 'hex':
			res = wrapEach(StringUtils.toHexArray(msg));
			break;
		case 'decimal':
			res = wrapEach(StringUtils.toDecimalArray(msg));
			break;
		case 'binary':
			res = wrapEach(StringUtils.toBinaryArray(msg));
			break;
	}
	return res;
};

WiseGuiConsoleView.prototype.onWebSocketMessageEvent = function(event) {

	var self = this;
	var message = JSON.parse(event.data);

	if (!message.type) {
		console.log('Received message with unknown content: ' + event.data);
		return;
	}

	var paused = this.view.find('#pause-output').is('.active');
	var reservation;
	
	if (message.type == 'upstream'  && !paused) {

		// append new message if in whitelist or whitelist empty
		var outputsFilterNodes = self.getOutputsFilterNodes();
		if (outputsFilterNodes.length === 0 || outputsFilterNodes.indexOf(message.sourceNodeUrn) > -1) {
			self.outputs[self.outputs.length] = message;
			// queue throttled redraw (keeps GUI responsive)
			setTimeout(function(){ self.throttledRedraw(); }, 5);
		}
		
	} else if (message.type == 'notification') {

		var blockAlertActions = null;
		var blockAlertMessage = $(
			'<div><strong>Backend notification at ' + message.timestamp + ':</strong><br/>' + message.message + '</div>'
		);

		if (getNavigationData().experimentId != this.reservation.experimentId) {

			var goToReservationButton = $('<button class="btn btn-primary">Go to reservation</button>');
			blockAlertActions = [goToReservationButton];
			goToReservationButton.bind('click', this, function(e, data) {
				navigateTo(self.experimentId);
			});

		}

		WiseGui.showInfoBlockAlert(blockAlertMessage, blockAlertActions);

	} else if (message.type == 'devicesAttached') {
		WiseGui.showInfoBlockAlert('Devices [' + message.nodeUrns.join(', ') + '] were attached at ' + message.timestamp);
	} else if (message.type == 'devicesDetached') {
		WiseGui.showInfoBlockAlert('Devices [' + message.nodeUrns.join(', ') + '] were detached at ' + message.timestamp);
	} else if (message.type == 'reservationStarted') {
		
		reservation = new WisebedReservation(message.reservationData);
		$(window).trigger(WiseGuiEvents.EVENT_RESERVATION_STARTED, reservation);

		if (reservation.experimentId != getNavigationData().experimentId) {
			var button = $('<input class="btn" type="button" value="Take me there">');
			button.bind('click', reservation, function(e) {
				navigateTo(e.data.experimentId);
			});
			WiseGui.showInfoBlockAlert('Reservation started at ' + message.timestamp, [button]);
		}

	} else if (message.type == 'reservationCancelled') {

		reservation = new WisebedReservation(message.reservationData);
		$(window).trigger(WiseGuiEvents.EVENT_RESERVATION_CANCELLED, reservation);
		WiseGui.showInfoBlockAlert('Reservation cancelled at ' + message.timestamp);

	} else if (message.type == 'reservationEnded') {

		reservation = new WisebedReservation(message.reservationData);
		$(window).trigger(WiseGuiEvents.EVENT_RESERVATION_ENDED, reservation);
		WiseGui.showInfoBlockAlert('Reservation ended at ' + message.timestamp);

	} else if (message.type == 'reservationFinalized') {

		reservation = new WisebedReservation(message.reservationData);
		$(window).trigger(WiseGuiEvents.EVENT_RESERVATION_FINALIZED, reservation);
		WiseGui.showInfoBlockAlert('Reservation finalized at ' + message.timestamp);
	}
};

WiseGuiConsoleView.prototype.onWebSocketOpen = function(event) {

};

WiseGuiConsoleView.prototype.onWebSocketClose = function(event) {

};

WiseGuiConsoleView.prototype.connectToReservation = function() {

	window.WebSocket = window.MozWebSocket || window.WebSocket;

	if (window.WebSocket) {

		var self = this;

		this.socket = new WebSocket(wisebedWebSocketBaseUrl + '/experiments/' + this.reservation.experimentId);

		this.socket.onmessage = function(event) {self.onWebSocketMessageEvent(event); };
		this.socket.onopen    = function(event) {self.onWebSocketOpen(event);         };
		this.socket.onclose   = function(event) {self.onWebSocketClose(event);        };

	} else {
		alert("Your browser does not support Web Sockets.");
	}
};

WiseGuiConsoleView.prototype.updateOutputsFilterNodesButton = function() {
	var filtered =  this.outputsFilterNodeSelectionDialog.areSomeSelected() &&
			       !this.outputsFilterNodeSelectionDialog.areAllSelected();
	this.outputsFilterButton.toggleClass('btn-info', filtered);
};

WiseGuiConsoleView.prototype.getOutputsFilterNodes = function() {
	return this.outputsFilterNodeSelectionDialog.getSelection();
};

module.exports = WiseGuiConsoleView;
},{"./string-utils.js":14,"./wisegui-events.js":16,"./wisegui-nodeselection-dialog.js":25}],16:[function(require,module,exports){

var WiseGuiEvents = {
	EVENT_EVENTWEBSOCKET_CONNECTED       : 'wisegui-eventwebsocket-connected',
	EVENT_EVENTWEBSOCKET_DISCONNECTED    : 'wisegui-eventwebsocket-disconnected',
	EVENT_DEVICES_ATTACHED               : 'wisegui-devices-attached-event',
	EVENT_DEVICES_DETACHED               : 'wisegui-devices-detached-event',
	EVENT_LOGGED_IN                      : 'wisegui-logged-in',
	EVENT_LOGGED_OUT                     : 'wisegui-logged-out',
	EVENT_LOGIN_ERROR                    : 'wisegui-login-error',
	EVENT_NAVIGATION                     : 'wisegui-navigation-event',
	EVENT_NODESELECTIONS_STORAGE_CHANGED : 'wisegui-nodeselections-storage-changed',
	EVENT_NOTIFICATION                   : 'wisegui-notification',
	EVENT_RESERVATION_ADDED              : 'wisegui-reservation-added',
	EVENT_RESERVATION_STARTED            : 'wisegui-reservation-started',
	EVENT_RESERVATION_CANCELLED          : 'wisegui-reservation-cancelled',
	EVENT_RESERVATION_ENDED              : 'wisegui-reservation-ended',
	EVENT_RESERVATION_FINALIZED          : 'wisegui-reservation-finalized',
	EVENT_RESERVATIONS_CHANGED           : 'wisegui-reservations-changed'
};

module.exports = WiseGuiEvents;
},{}],17:[function(require,module,exports){
var WiseGuiNodeSelectionDialog = require('./wisegui-nodeselection-dialog.js');
var WiseGuiOperationProgressView = require('./wisegui-operation-progress-view.js');

/**
 * #################################################################
 * WiseGuiFlashView
 * #################################################################
 */

var WiseGuiFlashView = function(reservation) {
	
	this.reservation              = reservation;
	this.experimentId             = reservation.experimentId;
	this.configurations           = [];

	this.addSetButton             = null;
	this.removeSetButton          = null;
	this.loadConfigurationButton  = null;
	this.saveConfigurationButton  = null;
	this.flashButton              = null;
	this.configurationsTableBody  = null;
	this.view                     = null;

	this.buildView();
};

WiseGuiFlashView.prototype.buildView = function() {
	this.view = $(
		'<div class="row">' +
		'	<div class="span3">' +
		'		<button class="btn WiseGuiFlashViewAddSetButton"> + </button>' +
		'		<button class="btn WiseGuiFlashViewRemoveSetButton"> - </button>' +
		'		<button class="btn WiseGuiFlashViewLoadConfigurationButton">Load</button>' +
		'		<button class="btn WiseGuiFlashViewSaveConfigurationButton">Save</button>' +
		'	</div>' +
		'	<div class="span3">' +
		'		<button class="btn btn-primary WiseGuiFlashViewFlashButton span2">Flash</button>' +
		'	</div>' +
		'</div>' +
		'<div class="row">' +
		'	<div class="span12">' +
		'		<table class="table table-striped">' +
		'			<thead>' +
		'				<tr>' +
		'					<th>Set</th>' +
		'					<th>Selected Nodes</th>' +
		'					<th>Image File</th>' +
		'					<th class="span5"></th>' +
		'				</tr>' +
		'			</thead>' +
		'			<tbody>' +
		'			</tbody>' +
		'		</table>' +
		'	</div>' +
		'</div>');

	this.addSetButton            = this.view.find('button.WiseGuiFlashViewAddSetButton').first();
	this.removeSetButton         = this.view.find('button.WiseGuiFlashViewRemoveSetButton').first();
	this.loadConfigurationButton = this.view.find('button.WiseGuiFlashViewLoadConfigurationButton').first();
	this.saveConfigurationButton = this.view.find('button.WiseGuiFlashViewSaveConfigurationButton').first();
	this.flashButton             = this.view.find('button.WiseGuiFlashViewFlashButton').first();
	this.configurationsTableBody = this.view.find('table tbody').first();

	var self = this;

	// bind actions for flash tab buttons
	this.addSetButton.bind('click', this.addConfiguration.bind(this));
	this.removeSetButton.bind('click', this.removeConfiguration.bind(this));
	this.loadConfigurationButton.bind('click', this.loadConfiguration.bind(this, this.loadConfigurationButton));
	this.saveConfigurationButton.bind('click', this.saveConfiguration.bind(this));
	this.flashButton.bind('click', this.executeFlashNodes.bind(this));
	
	this.addConfiguration();

	WiseGui.bindToReservationState(this.view.find('button'), this.experimentId);
};

WiseGuiFlashView.prototype.getFormData = function() {

	var formData = {
		configurations : []
	};

	for (var i=0; i<this.configurations.length; i++) {
		formData.configurations.push(this.configurations[i].config);
	}

	return formData;
};

WiseGuiFlashView.prototype.saveConfiguration = function(button) {

	var json = {
		configurations : []
	};

	for (var i=0; i<this.configurations.length; i++) {
		json.configurations.push(this.configurations[i].config);
	}
	var jsonString = JSON.stringify(json);

	//if(window.MozBlobBuilder) {
	//	var uriContent = "data:application/octet-stream;base64," + btoa(json);
	//	//window.open(uriContent, 'configuration.json');
	//	window.location = uriContent;
	//} else {
		var bb = new BlobBuilder();
		bb.append(jsonString);
		saveAs(bb.getBlob("text/plain;charset=utf-8"), "configuration.json");
	//}
};

WiseGuiFlashView.prototype.loadConfiguration = function(button) {

	button.attr("disabled", "true");

	var self = this;

	// @param: Type is conf-object
	var dialog;
	dialog = new WiseGuiLoadConfigurationDialog(
			function(data, textStatus, jqXHR) {

				dialog.hide();
				button.removeAttr("disabled");

				if(data === null) {
					return;
				}

				var configurations = data.configurations;

				// Reset
				self.configurationsTableBody.empty();
				self.configurations = [];

				// Iterate all configurations
				for(var i = 0; i < configurations.length; i++) {
					self.addConfiguration(configurations[i]);
				}
			},
			function(jqXHR, textStatus, errorThrown) {

				dialog.hide();
				button.removeAttr("disabled");
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
	dialog.view.on('hide', function() {
		button.removeAttr("disabled");
	});
	dialog.show();
};

// @see: http://stackoverflow.com/a/5100158/605890
WiseGuiFlashView.prototype.dataURItoBlob = function(dataURI) {
    // convert base64 to raw binary data held in a string
    // doesn't handle URLEncoded DataURIs
    var byteString = atob(dataURI.split(',')[1]);

    // separate out the mime component
    var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];

    // write the bytes of the string to an ArrayBuffer
    var ab = new ArrayBuffer(byteString.length);
    var ia = new Uint8Array(ab);
    for (var i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }

    // write the ArrayBuffer to a blob, and you're done
    var bb = new BlobBuilder();
    bb.append(ab);
    return bb.getBlob(mimeString);
};

WiseGuiFlashView.prototype.addConfiguration = function(conf) {

	// build and append the gui elements
	var nodeSelectionButton   = $('<button class="btn nodeSelectionButton span3">Select Nodes</button>');
	var imageFileInput        = $('<input type="file" style="opacity: 0; width: 0px; position:absolute; top:-100px;"/>');
	var imageFileButton       = $('<button class="btn fileSelectionButton span3">Select Image</button>');
	var imageFileInfoLabel    = $('<div/>');
	var tr                    = $('<tr/>');

	var setNumberTd           = $('<td>' + (this.configurations.length + 1) + '</td>');
	var nodeSelectionButtonTd = $('<td/>');
	var imageFileInputTd      = $('<td/>');
	var imageFileInfoLabelTd  = $('<td/>');

	nodeSelectionButtonTd.append(nodeSelectionButton);

	imageFileInputTd.append(imageFileInput);
	imageFileInputTd.append(imageFileButton);
	imageFileInfoLabelTd.append(imageFileInfoLabel);

	tr.append(setNumberTd, nodeSelectionButtonTd, imageFileInputTd, imageFileInfoLabelTd);
	this.configurationsTableBody.append(tr);

	// build and remember the configuration
	var configuration = {
		nodeSelectionButton : nodeSelectionButton,
		imageFileInput      : imageFileInput,
		imageFileButton     : imageFileButton,
		imageFileLabel      : imageFileInfoLabel,
		tr                  : tr,
		config              : { nodeUrns : null, image : null }
	};

	if(typeof(conf) == "object") {
		// Set the image
		if(conf.image !== null) {
			configuration.config.image = conf.image;
			var blob = this.dataURItoBlob(configuration.config.image);
			imageFileInfoLabel.append('<strong>' + blob.name + '</strong> (' + (blob.type || 'n/a') + ')<br/>' + blob.size + ' bytes');
		}
		// Set the node URNs
		if(conf.nodeUrns !== null) {

			var checkNodes = function(data) {

				var reservedNodeUrns = [];
				for(var i = 0; i < data.setup.node.length; i++) {
					reservedNodeUrns.push(data.setup.node[i].id);
				}

				var preSelectedNodeUrns = [];
				for(var k = 0; k < conf.nodeUrns.length; k++) {
					if($.inArray(conf.nodeUrns[k], reservedNodeUrns) >= 0) {
						preSelectedNodeUrns.push(conf.nodeUrns[k]);
					}
				}

				configuration.config.nodeUrns = preSelectedNodeUrns;

				var nodeSelectionButtonText = configuration.config.nodeUrns.length == 1 ?
						'1 node selected' :
						configuration.config.nodeUrns.length + ' nodes selected';

				nodeSelectionButton.html(nodeSelectionButtonText);
			};

			wisebed.getWiseMLAsJSON(this.reservation.experimentId, checkNodes, WiseGui.showAjaxError);
		}
	}

	this.configurations.push(configuration);

	// bind actions to buttons
	var self = this;

	nodeSelectionButton.bind('click', function() {

		var nodeSelectionDialog = new WiseGuiNodeSelectionDialog(
				self.experimentId,
				'Select Nodes',
				'Please select the nodes you want to flash.',
				configuration.config.nodeUrns
		);

		nodeSelectionButton.attr('disabled', true);
		nodeSelectionDialog.dialogDiv.on('hidden', function() {
			nodeSelectionButton.attr('disabled', false);
		});
		nodeSelectionDialog.show(
			function(nodeUrns) {
				nodeSelectionButton.attr('disabled', false);
				configuration.config.nodeUrns = nodeUrns;
				nodeSelectionButton.html((nodeUrns.length == 1 ? '1 node selected' : (nodeUrns.length + ' nodes selected')));
			},
			function() {}
		);
	});

	imageFileButton.bind('click', function() {
		configuration.imageFileInput.click();
	});

	imageFileInput.bind('change', function() {

		var imageFile       = imageFileInput[0].files[0];
		var imageFileReader = new FileReader();

		imageFileReader.onerror = function(progressEvent) {
			configuration.config.image = null;
			WiseGui.showWarningAlert('The file "' + imageFile.name+ '" could not be read!');
		};

		imageFileReader.onloadend = function(progressEvent) {
			configuration.config.image = imageFileReader.result;
			imageFileInfoLabel.empty();
			imageFileInfoLabel.append(
					'<strong>' + imageFile.name + '</strong> (' + (imageFile.type || 'n/a') + ')<br/>' + imageFile.size + ' bytes'

					// last modified: ' +
					// imageFile.lastModifiedDate.toLocaleDateString()
					//
					// Crashes in FF. Even if the File interface specifies a
					// lastModifiedDate,
					// it is not working/existing in FF.
					//
					// @see https://github.com/wisebed/rest-ws/issues/32
			);
		};

		imageFileReader.readAsDataURL(imageFile);

	});

	return configuration;
};

WiseGuiFlashView.prototype.removeConfiguration = function() {
	if (this.configurations.length > 1) {
		var configuration = this.configurations.pop();
		configuration.tr.remove();
		return configuration;
	}
	return null;
};

WiseGuiFlashView.prototype.executeFlashNodes = function() {

	var formData = this.getFormData();

	var allNodeUrns = [];
	$.each(formData.configurations, function(index, configuration) {
		$.each(configuration.nodeUrns, function(index, nodeUrn) {
			allNodeUrns.push(nodeUrn);
		});
	});

	var progressViewer = new WiseGuiOperationProgressView(
			allNodeUrns, 100,
			"All nodes were successfully flashed."
	);

	var self = this;
	self.flashButton.attr('disabled', true);
	
	wisebed.experiments.flashNodes(
			this.reservation.experimentId,
			formData,
			function(result) {
				self.flashButton.attr('disabled', false);
				progressViewer.update(result);
			},
			function(progress) {
				progressViewer.update(progress);
			},
			function(jqXHR, textStatus, errorThrown) {
				self.flashButton.attr('disabled', false);
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
};

/**
 * #################################################################
 * WiseGuiLoadConfigurationDialog
 * #################################################################
 */

var WiseGuiLoadConfigurationDialog = function(onSuccess, onError) {
	this.onSuccess = onSuccess;
	this.onError = onError;
	this.table = null;
	this.view = $('<div id="WiseGuiLoadDialog" class="modal hide"></div>');
	this.buildView();
};

WiseGuiLoadConfigurationDialog.prototype.hide = function() {
	this.view.modal('hide');
	this.view.remove();
};

WiseGuiLoadConfigurationDialog.prototype.show = function() {
	$(document.body).append(this.view);
	this.view.modal('show');
};

WiseGuiLoadConfigurationDialog.prototype.buildView = function() {

	var self = this;

	function handleError(errorMessage, input) {
		input.addClass('error');
		alert("Error: " + (errorMessage.length > 500 ? (errorMessage.substr(0, 500) + " [...]") : errorMessage));
		okButton.removeAttr('disabled');
		cancelButton.removeAttr('disabled');
	}

	function loadFromURL() {
		wisebed.experiments.getConfiguration(
				$.trim(input_url.val()),
				function(data, textStatus, jqXHR) {
					self.onSuccess(data, textStatus, jqXHR);
				},
				function(jqXHR, textStatus, errorThrown) {
					handleError(jqXHR.responseText, input_url);
				}
		);
	}

	function loadFromFile() {

		var files = document.getElementById('input_file').files;
		var f = files[0];

		if(f !== "") {
			var fileReader = new FileReader();
			fileReader.onloadend = function(progressEvent) {
				try {
					self.onSuccess(JSON.parse(fileReader.result));
				} catch(e) {
					handleError(e, input_file);
				}
			};
			fileReader.readAsText(f);
		} else {
			handleError("No file chosen", input_file);
		}
	}

	/*
	 * Dialog Header
	 */
	var dialogHeader = $('<div class="modal-header"><h3>Load a configuration</h3></div>');

	/*
	 * Dialog Body
	 */
	var dialogBody = $('<div class="modal-body"/>');

	var url = "";

	var form = $('<form class="form-horizontal"/>');

	var control_group_url = $('<div class="control-group"/>');
	var label_url = $('<label for="type_url" class="control-label">URL:</label>');
	var controls_url = $('<div class="controls"/>');
	var input_checkbox_url  = $('<input type="radio" name="type" id="type_url" value="url" checked>');
	var input_url = $('<input type="text" value="' + url + '" id="input_url" />');

	var control_group_file = $('<div class="control-group"/>');
	var label_file = $('<label for="type_file" class="control-label">File:</label>');
	var controls_file = $('<div class="controls"/>');
	var input_checkbox_file = $('<input type="radio" name="type" id="type_file" value="file">');
	var input_file = $('<input type="file" id="input_file"/>');

	input_url.focusin(
		function() {
			input_checkbox_file.attr('checked', false);
			input_checkbox_url.attr('checked', true);
		}
	);

	input_file.change(
		function() {
			input_checkbox_url.attr('checked', false);
			input_checkbox_file.attr('checked', true);
		}
	);

	controls_url.append(input_checkbox_url, input_url);
	control_group_url.append(label_url, controls_url);
	controls_file.append(input_checkbox_file, input_file);
	control_group_file.append(label_file, controls_file);
	form.append(control_group_url, control_group_file);
	dialogBody.append(form);

	/*
	 * Dialog Footer
	 */
	var dialogFooter = $('<div class="modal-footer"/>');

	var okButton = $('<input class="btn btn-primary" value="OK" style="width:35px;text-align:center;"/>');
	var cancelButton = $('<input class="btn" value="Cancel" style="width:45px;text-align:center;"/>');

	okButton.bind('click', this, function(e) {

		okButton.attr("disabled", "true");
		cancelButton.attr("disabled", "true");

		// Check, which radio button is used
		var val = dialogBody.find("input:checked").val();
		if(val == "url") {
			loadFromURL.bind(self)();
		} else if (val == "file") {
			loadFromFile.bind(self)();
		}
	});

	cancelButton.bind('click', this, function(e) {
		self.onSuccess(null);
	});

	dialogFooter.append(okButton, cancelButton);

	/**
	 * Build view
	 */
	this.view.append(dialogHeader, dialogBody, dialogFooter);
};

module.exports = WiseGuiFlashView;
},{"./wisegui-nodeselection-dialog.js":25,"./wisegui-operation-progress-view.js":28}],18:[function(require,module,exports){
/**
 * Represents a location
 */
function Coordinate(latitude, longitude, x, y, z, phi, theta, rho) {

	// geographical coordinates
	this.latitude = latitude;
	this.longitude = longitude;

	// cartesian coordinates
	this.x = x;
	this.y = y;
	this.z = z;

	// spherical coordinates
	this.phi = phi;
	this.theta = theta;
	this.rho = rho;
}

/**
 * Represents a node and its location
 */
function Node(id, desc, c) {
	this.id = id;
	this.desc = desc;
	this.c = c;
	this.hasLatLng = function() {
		return c.latitude !== undefined && c.longitude !== undefined;
	};
}

/**
 * Parses WiseML
 */
var WiseGuiGoogleMapsView = function(wisemlParameter, parentDiv) {
	this.wiseml = wisemlParameter;
	this.origin = null;
	this.nodes = [];
	this.markersArray = [];
	this.map = null;
	this.infoWindows = {};
	this.view = null;
	this.parse();
	this.buildView(parentDiv);

};

WiseGuiGoogleMapsView.prototype.hasOrigin = function() {
	return this.origin.latitude !== undefined && this.origin.longitude !== undefined;
};

WiseGuiGoogleMapsView.prototype.parse = function() {

	if (!this.wiseml.setup || !this.wiseml.setup.origin || !this.wiseml.setup.origin.outdoorCoordinates) {
		this.origin = new Coordinate(undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined);
	} else {
		var c = this.wiseml.setup.origin.outdoorCoordinates;
		this.origin = new Coordinate(
				(c.latitude === null ? undefined : c.latitude),
				(c.longitude === null ? undefined : c.longitude),
				(c.x === null ? undefined : c.x),
				(c.y === null ? undefined : c.y),
				(c.z === null ? undefined : c.z),
				(c.phi === null ? undefined : c.phi),
				(c.theta === null ? undefined : c.theta),
				(c.rho === null ? undefined : c.rho)
		);
	}

	this.wiseml.setup.node.forEach(this.addNodeIfHasPosition, this);
};

WiseGuiGoogleMapsView.prototype.addNodeIfHasPosition = function(node) {
	if (!node.position) {
		console.log("Not adding node \"%s\" to map as position data is missing", node.id);
	} else if (node.position.indoorCoordinates) {
		console.log("Not adding node \"%s\" to map as indoor coordinates are not supported", node.id);
	} else if (node.position.outdoorCoordinates) {
		var id = node.id;
		var coordinate = new Coordinate(
				node.position.outdoorCoordinates.latitude,
				node.position.outdoorCoordinates.longitude,
				node.position.outdoorCoordinates.x,
				node.position.outdoorCoordinates.y,
				node.position.outdoorCoordinates.z,
				node.position.outdoorCoordinates.phi,
				node.position.outdoorCoordinates.theta,
				node.position.outdoorCoordinates.rho
		);
		this.nodes.push(new Node(id, node.description, coordinate));
	}
};

WiseGuiGoogleMapsView.prototype.hasNodePositions = function() {
	for (var i=0; i<this.nodes.length; i++) {
		if (this.nodes[i].hasLatLng()) {
			return true;
		}
	}
	return false;
};

WiseGuiGoogleMapsView.prototype.buildView = function(parentDiv) {
	
	var hasNodePositions = this.hasNodePositions();
	var hasOrigin = this.hasOrigin();

	if (hasNodePositions || hasOrigin) {

		this.view = $('<div class="gMap" style="height:500px;"/>');
		parentDiv.append(this.view);

		this.initMap();

		// Delete old overlays
		this.deleteOverlays();

		if (hasNodePositions) {
			this.nodes.forEach(this.addMarker, this);
		}

		// Adjust map
		this.setBounds();
	} else {
		this.view = $(
			'<div class="gMap" style="height:500px;">' +
			'	<span class="label label-warning">Map View Not Available</span>' +
			'	<p>' +
			'		The testbed meta data does not contain any information about the testbeds location or the location ' +
			'		of the nodes therein. Therefore the map can not be rendered.' +
			'	</p>' +
			'</div>');
		parentDiv.append(this.view);
	}
};

/**
 * Calculates latitude and longitude for a node.
 *
 * @param node a google.maps.LatLng instance
 */
WiseGuiGoogleMapsView.prototype.getLatLng = function(node) {
	if (node.c.latitude && node.c.longitude) {
		return new google.maps.LatLng(node.c.latitude, node.c.longitude);
	} else if (node.c.x && node.c.y && node.c.z) {
		console.log('Ignoring cartesian coordinates for map view for "%s": not implemented', node.id);
		return null;
	} else if (node.c.phi && node.c.theta && node.c.rho) {
		console.log('Ignoring spherical coordinates for map view for "%s": not implemented', node.id);
		return null;
	}
	return null;
};

/**
 * Adds a Marker to the map
 *
 */
WiseGuiGoogleMapsView.prototype.addMarker = function(node) {

	var markerLatLng = this.getLatLng(node);

	if (!markerLatLng) {
		return;
	}

	var marker = new google.maps.Marker({
		position : markerLatLng,
		map      : this.map,
		urn      : node.id,
		icon     : 'img/maps/red-dot.png'
	});

	this.infoWindows[node.id] = new google.maps.InfoWindow();
	this.infoWindowRenderer = function(key,value) {
		if (value === null || value === undefined) {
			return undefined;
		} else {
			return value;
		}
	};

	var infoWindowContent = '<h5>' + node.id + '</h5>' +
					(JSON.stringify(node.c, this.infoWindowRenderer, '  ').replace(/\n/g, "<br/>") + '</br>') +
					(node.desc ? node.desc + '</br>' : '');
	this.infoWindows[node.id].setContent(infoWindowContent);

	var self = this;
	
	this.markersArray.push(marker);
	this.mapSpiderfier.addMarker(marker);
	this.markerCluster.addMarker(marker);
};

/**
 * Initializes the google map
 *
 */
WiseGuiGoogleMapsView.prototype.initMap = function() {

	var self = this;
	var latlng = this.hasOrigin() ?
			new google.maps.LatLng(this.origin.latitude, this.origin.longitude) :
			new google.maps.LatLng(0, 0);

	var myOptions = {
		zoom : 17,
		center : latlng,
		mapTypeId : google.maps.MapTypeId.HYBRID
	};

	this.markersArray = [];
	this.map = new google.maps.Map(this.view.get()[0], myOptions);

	var spiderfierOptions = {
		markersWontMove: false,
		markersWontHide: true,
		keepSpiderfied:  true,
		nearByDistance:  10
	};
	this.mapSpiderfier = new OverlappingMarkerSpiderfier(this.map, spiderfierOptions);

	var clusterOptions = {
		//maxZoom:     19,
		gridSize:    10,
		zoomOnClick: false
    };
	this.markerCluster = new MarkerClusterer(this.map, [], clusterOptions);

	// handle clicks on clusters: zoom to cluster or spiderfy if near enough
	google.maps.event.addListener(this.markerCluster, "click", function (c) {

		var hideAndSpiderfy = function() {
			var clustered = c.getMarkers();
			$.each(clustered, function(index, marker) {
				marker.setMap(self.map);
			});
			c.clusterIcon_.hide();
			google.maps.event.trigger(clustered[0], 'click');
		};

		if ( self.map.getZoom() > 19 ) {
			hideAndSpiderfy();
		} else {
			self.map.fitBounds(c.getBounds());
		}
		
	});

	this.mapSpiderfier.addListener('unspiderfy', function(markers) {
		self.markerCluster.repaint();
	});
	
	this.mapSpiderfier.addListener('click', function(marker, event) {
		for (var nodeUrn in self.infoWindows) {
			self.infoWindows[nodeUrn].close();
		}
		self.infoWindows[marker.urn].open(self.map, marker);
	});
};

/**
 * Deletes all markers in the array by removing references to them
 *
 */
WiseGuiGoogleMapsView.prototype.deleteOverlays = function() {

	$.each(this.markersArray, function(index, marker) {
		marker.setMap(null);
	});

	this.markersArray = [];
	//this.mapSpiderfier.clearMarkers();
	//this.markerCluster.clearMarkers();
};

/**
 * Centers, pans and zooms the map such that all markers are visible
 *
 */
WiseGuiGoogleMapsView.prototype.setBounds = function() {

	if (this.markersArray.length > 1) {
		var bounds = new google.maps.LatLngBounds();

		$.each(this.markersArray, function(index, marker) {
			bounds.extend(marker.getPosition());
		});

		this.map.fitBounds(bounds);

	} else if (this.markersArray.length == 1) {
		this.map.setCenter(this.markersArray[0].getPosition());
	}
};

/**
 * Produces rdf from the nodes
 *
 */
WiseGuiGoogleMapsView.prototype.rdf = function() {
	var rdf = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

	for (var j = 0; j < nodes.length; j++) {
		rdf += "<" + nodes[j].id + "> ";
		rdf += "<" + predLat + "> ";
		rdf += "\"" + nodes[j].c.x + "\"^^xsd:double .\n";
		rdf += "<" + nodes[j].id + "> ";
		rdf += "<" + predLong + "> ";
		rdf += "\"" + nodes[j].c.y + "\"^^xsd:double .\n";
	}
	return rdf;
};

module.exports = WiseGuiGoogleMapsView;

},{}],19:[function(require,module,exports){
var WiseGuiEvents = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiLoginDialog
 * #################################################################
 */
var WiseGuiLoginDialog = function() {

	this.loginFormRows = [];
	this.loginData = { authenticationData : [] };

	this.view = $('<div id="WiseGuiLoginDialog" class="modal hide"></div>');
	$(document.body).append(this.view);

	this.okButton = null;
	this.cancelButton = null;

	this.buildView();

	var self = this;
	$(window).bind(WiseGuiEvents.EVENT_LOGIN_ERROR, function(e, data) { self.onLoginError(data); });
	$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function() { self.onLoginSuccess(); });
	$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function() { self.onLoginSuccess(); });
};

WiseGuiLoginDialog.prototype.onLoginError = function(data) {

	var self = this;

	if (data.jqXHR.status != 403) {
		console.log(data.jqXHR);
		WiseGui.showAjaxError(data.jqXHR, data.textStatus, data.errorThrown);
		window.setTimeout(function() {self.hide();}, 1000);
	}

	$.each(this.loginFormRows, function(index, elem) {
		$(elem.inputUsername).removeClass('success');
		$(elem.inputPassword).removeClass('success');
		$(elem.inputUsername).addClass('error');
		$(elem.inputPassword).addClass('error');
		self.okButton.removeAttr("disabled");
		self.cancelButton.removeAttr("disabled");
	});
};

WiseGuiLoginDialog.prototype.onLoginSuccess = function() {

	var self = this;

	$.each(this.loginFormRows, function(index, elem) {
		$(elem.inputUsername).removeClass('error');
		$(elem.inputPassword).removeClass('error');
		$(elem.inputUsername).addClass('success');
		$(elem.inputPassword).addClass('success');
	});

	window.setTimeout(function() {self.hide();}, 1000);
};

WiseGuiLoginDialog.prototype.hide = function() {
	this.view.modal('hide');
};

WiseGuiLoginDialog.prototype.show = function() {
	this.view.modal('show');
};

WiseGuiLoginDialog.prototype.updateLoginDataFromForm = function() {

	for (var i=0; i<this.loginFormRows.length; i++) {

		this.loginData.authenticationData[i] = {
			urnPrefix : this.loginFormRows[i].inputUrnPrefix.value,
			username  : this.loginFormRows[i].inputUsername.value,
			password  : this.loginFormRows[i].inputPassword.value
		};
	}
};

WiseGuiLoginDialog.prototype.addRowToLoginForm = function(tbody, urnPrefix, username, password) {

	var that = this;
	var tr = $('<tr/>');
	var i = this.loginFormRows.length;

	var inputUrnPrefix = $('<input type="text" id="urnprefix'+i+'" name="urnprefix'+i+'" value="'+urnPrefix+'" readonly/>');
	var inputUsername = $('<input type="text" id="username'+i+'" name="username'+i+'" value="'+username+'"/>');
	var inputPassword = $('<input type="password" id="password'+i+'" name="password'+i+'" value="'+password+'"/>');

	inputUsername.keyup(function(e) {
		if ((e.keyCode || e.which) == 13) {
			that.startLogin();
		}
	});

	inputPassword.keyup(function(e) {
		if ((e.keyCode || e.which) == 13) {
			that.startLogin();
		}
	});

	this.loginFormRows[this.loginFormRows.length] = {
		"tr" : tr,
		"inputUrnPrefix" : inputUrnPrefix[0],
		"inputUsername" : inputUsername[0],
		"inputPassword" : inputPassword[0]
	};

	var tdUrnPrefix = $('<td/>');
	var tdUsername = $('<td/>');
	var tdPassword = $('<td/>');

	tdUrnPrefix.append(inputUrnPrefix);
	tdUsername.append(inputUsername);
	tdPassword.append(inputPassword);

	tr.append($('<td>'+(this.loginFormRows.length)+'</td>'));
	tr.append(tdUrnPrefix);
	tr.append(tdUsername);
	tr.append(tdPassword);

	tbody.append(tr);
};

WiseGuiLoginDialog.prototype.buildView = function() {

	var that = this;

	var dialogHeader = $('<div class="modal-header"><h3>Login</h3></div>');

	var dialogBody = $(
		'<div class="modal-body WiseGuiLoginDialog"/>' +
		'		<form id="WiseGuiLoginDialogForm">' +
		'		<table class="table" id="WiseGuiLoginDialogFormTable">' +
		'			<thead>' +
		'				<tr>' +
		'					<th>Testbed</th>' +
		'					<th>URN Prefix</th>' +
		'					<th>Username</th>' +
		'					<th>Password</th>' +
		'				</tr>' +
		'			</thead>' +
		'			<tbody>' +
		'			</tbody>' +
		'		</table>' +
		'		</form>' +
		'	</div>');

	this.okButton = $('<input class="btn btn-primary" value="OK" style="width:25px;text-align:center;">');
	this.cancelButton = $('<input class="btn" value="Cancel" style="width:45px;text-align:center;">');

	this.cancelButton.bind('click', this, function(e) {
		e.data.hide();
	});

	this.okButton.bind('click', this, function(e) {
		that.startLogin();
	});

	var dialogFooter = $('<div class="modal-footer"/>');
	dialogFooter.append(this.cancelButton, this.okButton);
	this.view.append(dialogHeader, dialogBody, dialogFooter);

	var loginFormTableBody = this.view.find('#WiseGuiLoginDialogFormTable tbody');
	var urnPrefixes = testbedDescription.urnPrefixes;

	for (var i=0; i<urnPrefixes.length; i++) {
		var user = (localStorage[urnPrefixes[i]+'_user'] !== undefined) ? localStorage[urnPrefixes[i]+'_user'] : '';
		var pass = (localStorage[urnPrefixes[i]+'_pass'] !== undefined) ? localStorage[urnPrefixes[i]+'_pass'] : '';
		this.addRowToLoginForm(loginFormTableBody, urnPrefixes[i], user, pass );
	}

	
	var helpTextLocalStorage = 'Select this check box, log in and your credentials are stored ' +
		'<strong>unencrypted</strong> in your browser (HTML5 local storage). <br/>' +
		'<br/>' +
		'Uncheck the check box and log in to delete previously stored credentials.';

	var trStoreCredentials = $('<tr/>');
	var storeCredentials_checkbox;
	
	if(localStorage[this.loginFormRows[0].inputUrnPrefix.value+'_user'] !== undefined){
		storeCredentials_checkbox = $('<input type="checkbox" style="margin:3px" checked="checked">');
	}else{
		storeCredentials_checkbox = $('<input type="checkbox" style="margin:3px" ">');
	}

	storeCredentials_checkbox.popover({
		placement : 'bottom',
		trigger   : 'manual',
		animation : true,
		content   : helpTextLocalStorage,
		title     : "Caution!"
	});
	
	storeCredentials_checkbox.mouseover(
		function() {
			storeCredentials_checkbox.popover("show");
		}
	);

	storeCredentials_checkbox.mouseout(
		function() {
			storeCredentials_checkbox.popover("hide");
		}
	);
	
	var tdStoreCredentials = $('<td colspan="4"/>');
	tdStoreCredentials.append(storeCredentials_checkbox);
	tdStoreCredentials.append(" remember");
	trStoreCredentials.append(tdStoreCredentials);
	this.storeCredentials_checkbox = storeCredentials_checkbox;

	loginFormTableBody.append(trStoreCredentials);


	loginFormTableBody.append($(
			'<tr>' + 
			'	<td style="padding-bottom: 0" colspan="4">' +
			'		No account yet? <a href="/user_registration" target="_blank">Register here!' +
			'	</td>' +
			'</tr>')
	);
};

WiseGuiLoginDialog.prototype.startLogin= function() {

	this.okButton.attr("disabled", "true");
	this.cancelButton.attr("disabled", "true");

	if (this.storeCredentials_checkbox[0].checked) {
		for (var i=0; i<this.loginFormRows.length; i++) {
			localStorage[this.loginFormRows[i].inputUrnPrefix.value+'_user'] = this.loginFormRows[i].inputUsername.value;
			localStorage[this.loginFormRows[i].inputUrnPrefix.value+'_pass'] = this.loginFormRows[i].inputPassword.value;
		}
	} else {
		for (var j=0; j<this.loginFormRows.length; j++) {
			localStorage.removeItem(this.loginFormRows[j].inputUrnPrefix.value+'_user');
			localStorage.removeItem(this.loginFormRows[j].inputUrnPrefix.value+'_pass');
		}
	}

	this.updateLoginDataFromForm();
	doLogin(this.loginData);
};

module.exports = WiseGuiLoginDialog;
},{"./wisegui-events.js":16}],20:[function(require,module,exports){
var WiseGuiEvents = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiLoginObserver
 * #################################################################
 *
 * Listens to WiseGui events 'wisegui-logged-in' and 'wisegui-logged-out'
 * and tries to renew the login before it times out. Also, it triggers the
 * initial checking if the user is currently logged in upon application
 * reload and triggers 'wisegui-logged-in' and 'wisegui-logged-out' events
 * respectively so that other components can update their state.
 */

var WiseGuiLoginObserver = function() {
	this.loginData   = undefined;
	this.schedule    = undefined;
	this.interval    = 10 * 60 * 1000;
};

WiseGuiLoginObserver.prototype.renewLogin = function() {

	console.log('WiseGuiLoginObserver trying to renew login');

	var self = this;
	wisebed.login(
			this.loginData,
			function(){
				console.log('WiseGuiLoginObserver successfully renewed login');
			},
			function(jqXHR, textStatus, errorThrown) {
				console.log('WiseGuiLoginObserver failed renewing login');
				self.stopObservation();
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
};

WiseGuiLoginObserver.prototype.startObserving = function() {

	var self = this;

	$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function(e, data) {
		console.log('WiseGuiLoginObserver starting observation');
		if (data && data.loginData) {
			self.loginData = data.loginData;
			self.schedule = window.setInterval(self.renewLogin, self.interval);
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function(e, data) {
		if (self.schedule !== undefined) {
			console.log('WiseGuiLoginObserver stopping observation');
			window.clearInterval(self.schedule);
			self.schedule = undefined;
		}
	});

	checkLoggedIn(function(isLoggedIn) {
		$(window).trigger(isLoggedIn ? WiseGuiEvents.EVENT_LOGGED_IN : WiseGuiEvents.EVENT_LOGGED_OUT);
	});
};

WiseGuiLoginObserver.prototype.stopObserving = function() {
	console.log('WiseGuiLoginObserver.stopObserving()');
	if (this.schedule !== undefined) {
		window.clearInterval(this.schedule);
	}
};

module.exports = WiseGuiLoginObserver;
},{"./wisegui-events.js":16}],21:[function(require,module,exports){
var WiseGuiModalDialog = function(options, callbackOk, callbackCancel) {

	this.options = {
		title       : (options && options.title)       || 'Please Confirm',
		body        : (options && options.body)        || 'Are you sure?',
		labelCancel : (options && options.labelCancel) || 'Cancel',
		labelOk     : (options && options.labelOk)     || 'OK'
	};

	this.cancelled = true;
	this.view = $(
		'<div class="modal hide" id="myModal">' +
		'	<div class="modal-header">' +
		'		<button type="button" class="close" data-dismiss="modal">×</button>' +
		'		<h3>' + this.options.title + '</h3>' +
		'	</div>' +
		'	<div class="modal-body">' +
		'		<p>' + this.options.body + '</p>' +
		'	</div>' +
		'	<div class="modal-footer">' +
		'		<a href="#" class="btn btn-cancel" data-dismiss="modal">' + this.options.labelCancel + '</a>' +
		'		<a href="#" class="btn btn-ok btn-primary">' + this.options.labelOk + '</a>' +
		'	</div>' +
		'</div>'
	);
	
	this.view.find('.btn-ok').on('click', function(e) {
		e.preventDefault();
		this.cancelled = false;
		this.hide();
	}.bind(this));

	this.view.on('hidden', function() {
		if (this.cancelled && $.isFunction(callbackCancel)) {
			callbackCancel();
		} else if (!this.cancelled && $.isFunction(callbackOk)) {
			callbackOk();
		}
	}.bind(this));
};

WiseGuiModalDialog.prototype.show = function() {
	this.view.modal('show');
};

WiseGuiModalDialog.prototype.hide = function() {
	this.view.modal('hide');
};

WiseGuiModalDialog.prototype.wasCancelled = function() {
	return this.cancelled;
};

module.exports = WiseGuiModalDialog;
},{}],22:[function(require,module,exports){
var WiseGuiEvents      = require('./wisegui-events.js');
var WiseGuiLoginDialog = require('./wisegui-login-dialog.js');

/**
 * #################################################################
 * WiseGuiNavigationView
 * #################################################################
 */

var WiseGuiNavigationView = function() {

	this.view                 = null;
	this.primaryMenu          = null;
	this.secondaryMenu        = null;

	this.aboutButton          = null;
	this.loginButtonLi        = null;
	this.loginButton          = null;
	this.logoutButtonLi       = null;
	this.logoutButton         = null;
	this.reservationsButtonLi = null;
	this.reservationsButton   = null;

	this.buildView();
};

WiseGuiNavigationView.prototype.buildView = function() {

	this.view = $(
			'<div class="navbar">' +
			'		<div class="navbar-inner">' +
			'			<div class="container">' +
			'				<ul class="nav"/>' +
			'				<ul class="nav secondary-nav pull-right">' +
			'					<li class="WiseGuiNavAboutButton">' +
			'						<a href="#" data-toggle="modal" data-target="#aboutModal">About</a>' +
			'					</li>' +
			'				</ul>' +
			'			</div>' +
			'		</div>' +
			'	</div>' +
			'	<div id="aboutModal" class="modal hide">' +
			'		<div class="modal-header">' +
			' 		<h1>About WiseGui</h1>' +
			'		</div>' +
			'		<div class="modal-body">' +
			'			This is an open-source project published under the terms of the BSD license. Sources are freely available from' +
			'			<a href="https://github.com/wisebed/wisegui" target="_blank">github.com/wisebed/wisegui</a>.' +
			' 		<br/>' +
			' 		<br/>' +
		    '         <h3>Troubleshooting</h3>' +
		    '         If you experience any issues with this software please file a bug report at ' +
		    '         <a href="https://github.com/wisebed/wisegui/issues" target="_blank">github.com/wisebed/wisegui/issues</a>.' +
		    '         Please include the versioning information below.<br/>' +
		    '         <br/>' +
		    '         <h3>Backend Version Information</h3>' +
		    '         <table class="table table-striped table-condensed">' +
		    '             <tr>' +
    		'                 <td>Application Name:</td>' +
		    '                 <td>'+testbedDescription.appName+'</td>' +
		    '             </tr>' +
		    '             <tr>' +
		    '                 <td>Application Version:</td>' +
		    '                 <td>'+testbedDescription.appVersion+'</td>' +
		    '             </tr>' +
		    '             <tr>' +
		    '                 <td>Application Build:</td>' +
		    '                 <td>'+testbedDescription.appBuild+'</td>' +
		    '             </tr>' +
		    '             <tr>' +
		    '                 <td>Application Branch:</td>' +
		    '                 <td>'+testbedDescription.appBranch+'</td>' +
		    '             </tr>' +
		    '         </table>' +
			'			&copy; <a href="http://www.itm.uni-luebeck.de/people/bimschas/" target="_blank">Daniel Bimschas</a>,' +
			'			<a href="http://www.itm.uni-luebeck.de/people/ebers/" target="_blank">Sebastian Ebers</a>,' +
			'			<a href="http://www.itm.uni-luebeck.de/people/pfisterer/" target="_blank">Dennis Pfisterer</a>,' +
			'			<a href="http://www.itm.uni-luebeck.de/people/boldt/" target="_blank">Dennis Boldt</a>,' +
			'			<a href="http://www.itm.uni-luebeck.de/people/massel/" target="_blank">Florian Massel</a>,' +
			'			Philipp Abraham<br/>' +
			'		</div>' +
			'		<div class="modal-footer">' +
			'		</div>' +
			'	</div>'
	);

	this.primaryMenu   = this.view.find('ul.nav:not(ul.secondary-nav)').first();
	this.secondaryMenu = this.view.find('ul.secondary-nav').first();

	// create all buttons and attach them
	this.primaryMenu.append('<li class="WiseGuiNavOverviewButton"><a href="#">Overview</a></li>');

	this.overviewButtonLi         = this.primaryMenu.find('li.WiseGuiNavOverviewButton').first();
	this.overviewButton           = this.overviewButtonLi.find('a').first();

	this.secondaryMenu.append(
			  '<li class="WiseGuiNavReservationsButton"><a href="#">Make Reservation</a></li>' +
			'<li class="WiseGuiNavLogoutButton"><a href="#">Logout</a></li>' +
			'<li class="WiseGuiNavLoginButton"><a href="#">Login / Register</a></li>');

	this.reservationsButtonLi = this.secondaryMenu.find('li.WiseGuiNavReservationsButton').first();
	this.reservationsButton   = this.reservationsButtonLi.find('a').first();
	this.loginButtonLi        = this.secondaryMenu.find('li.WiseGuiNavLoginButton').first();
	this.loginButton          = this.loginButtonLi.find('a').first();
	this.logoutButtonLi       = this.secondaryMenu.find('li.WiseGuiNavLogoutButton').first();
	this.logoutButton         = this.logoutButtonLi.find('a').first();

	// hide all buttons
	this.loginButtonLi.hide();
	this.logoutButtonLi.hide();
	this.reservationsButtonLi.hide();

	var self = this;

	// bind actions to buttons
	this.overviewButton.bind('click', function(e) {
		e.preventDefault();
		if (getNavigationData().experimentId) {
			navigateTo();
		}
	});

	this.reservationsButton.bind('click', function(e) {
		e.preventDefault();
		showReservationsDialog();
	});

	this.logoutButton.bind('click', function(e) {
		e.preventDefault();
		doLogout();
	});

	this.loginButton.bind('click', function(e) {
		e.preventDefault();
		new WiseGuiLoginDialog().show();
	});

	// bind to login and logout events
	$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function() {
		self.loginButtonLi.hide();
		self.logoutButtonLi.show();
		self.reservationsButtonLi.show();
	});

	$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function() {
		self.loginButtonLi.show();
		self.logoutButtonLi.hide();
		self.reservationsButtonLi.hide();
	});
	
	$(window).bind(WiseGuiEvents.EVENT_NAVIGATION, function(e, navigationData) {
		if (navigationData.nav == 'experiment') {
			self.overviewButtonLi.removeClass('active');
		} else {
			self.overviewButtonLi.addClass('active');
		}
	});
};

module.exports = WiseGuiNavigationView;

},{"./wisegui-events.js":16,"./wisegui-login-dialog.js":19}],23:[function(require,module,exports){
var WiseGuiEvents = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiNodeStatusIcon
 * #################################################################
 */
var WiseGuiNodeStatusIcon = function(nodeUrn) {
	
	this.nodeUrn = nodeUrn;
	
	this.view = $('<img src="img/famfamfam/help.png" alt="'+nodeUrn+'" title="'+nodeUrn+'"/>');
	
	var self = this;

	$(window).bind(WiseGuiEvents.EVENT_DEVICES_ATTACHED, function(e, devicesAttachedEvent) {
		if (devicesAttachedEvent.nodeUrns.indexOf(nodeUrn) >= 0) {
			self.view.attr('src', 'img/famfamfam/tick.png');
		}
	});

	$(window).bind(WiseGuiEvents.EVENT_DEVICES_DETACHED, function(e, devicesDetachedEvent) {
		if (devicesDetachedEvent.nodeUrns.indexOf(nodeUrn) >= 0) {
			self.view.attr('src', 'img/famfamfam/cross.png');
		}
	});
};

module.exports = WiseGuiNodeStatusIcon;
},{"./wisegui-events.js":16}],24:[function(require,module,exports){
/**
 * #################################################################
 * WiseGuiNodeTable
 * #################################################################
 */

var WiseGuiNodeStatusIcon = require('./wisegui-node-status-icon.js');
var WiseGuiTable = require('./wisegui-table.js');
var WiseGuiEvents = require('./wisegui-events.js');

var WiseGuiNodeTable = function (wiseML, parent, showCheckboxes, showFilter) {
	this.table = null;
	this.wiseML = wiseML;
	this.showCheckboxes = showCheckboxes;
	this.showFilter = showFilter;
	this.parent = parent;
	this.generateTable();
};

WiseGuiNodeTable.prototype.generateTable = function () {

	var that = this;

	// The header
	var header = [' ', 'Node URN','Type','Position','Sensors'];

	var nodeUrns = this.wiseML.setup.node.map(function (node) { return node.id;	});
	var connectionStatus = {};
	nodeUrns.forEach(function(nodeUrn) {
		connectionStatus[nodeUrn] = $('<div class="connectionStatus" />');
		connectionStatus[nodeUrn].append(new WiseGuiNodeStatusIcon(nodeUrn).view);
	});

	if(nodeUrns.length === 0){
		console.warn("No sensor nodes found");
		return;
	}

	wisebed.experiments.areNodesConnected(nodeUrns, function(result) {

		var attached = [];
		var detached = [];

		for (var node in result) {
			if (result.hasOwnProperty(node) && result[node].statusCode == 1) {
				attached.push(node);
			}
			else {
				detached.push(node);
			}
		}

		// emulate devicesAttachedEvent
		if (attached.length > 0) {
			$(window).trigger(WiseGuiEvents.EVENT_DEVICES_ATTACHED, {
				type : 'devicesAttached',
				timestamp : new Date().toISOString(),
				nodeUrns : attached
			});
		}

		// emulate devicesDetachedEvent
		if (detached.length > 0) {
			$(window).trigger(WiseGuiEvents.EVENT_DEVICES_DETACHED, {
				type : 'devicesDetached',
				timestamp : new Date().toISOString(),
				nodeUrns : detached
			});
		}

	}, function(jqXHR, textStatus, errorThrown) {
		WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
	});

	// The row producer gives something like
	// ["", "id", "type", "(x,y,z)", "a,b,c"]
	var rowProducer = function (node) {
		var data = [];
		var capabilities = [];

		if(node.capability !== null) {
			for(var j = 0; j < node.capability.length; j++) {
				parts = explode(":", node.capability[j].name);
				capabilities[j] = parts[parts.length-1];
			}
		}

		data.push(connectionStatus[node.id]);
		data.push(node.id);
		data.push(node.nodeType);

		if (node.position && node.position.outdoorCoordinates) {

			var c = node.position.outdoorCoordinates;

			if (c.latitude && c.longitude) {
				data.push('(' + c.latitude + ',' + c.longitude+ ')');
			} else if (c.x && c.y && c.z) {
				data.push('[' + c.x + ',' + c.y + ',' + c.z + ']');
			} else if (c.x && c.y) {
				data.push('[' + c.x + ',' + c.y + ',0]');
			} else if (c.rho && c.phi && c.theta) {
				data.push(JSON.stringify({rho: c.rho, phi: c.phi, theta: c.theta}));
			} else {
				data.push('');
			}

		} else if (node.position && node.position.indoorCoordinates) {
			data.push(JSON.stringify(node.position.indoorCoordinates));
		} else {
			data.push('');
		}

		if(capabilities.length > 0) {
			data.push(capabilities.join("<br/>"));
		} else {
			data.push('');
		}

		return data;
	};

	// Use the usual table
	this.table = new WiseGuiTable(
		this.wiseML.setup.node,
		header,
		rowProducer,
		null,
		null,
		this.showCheckboxes,
		this.showFilter,
		{
			sortColumn  : 1,
			sortAsc     : true,
			sortHeaders : { 0 : {sorter : false}}
		}
	);

	// This vars store the predefined filters
	var predefined_filter_types = [];
	var predefined_filter_functions = [];

	// Add type filters
	$(this.wiseML.setup.node).each(
		function() {
			var t = this.nodeType;
			var text = "Only nodes of type " + t;
			if($.inArray(text, predefined_filter_types) < 0) {
				predefined_filter_types.push(text);
				var fn = function(e) {
					return e.nodeType == t;
				};
				predefined_filter_functions.push(fn);
			}
		}
	);

	// Other filters can be added here

	// Here the select will be generated
	var select = $('<select style="width:39%;background-color:#FFF;margin-left:1px;vertical-align:bottom;height:28px;"></select>');
	select.change(
		function () {
			var idx = parseInt($(this).val());
			var fn = predefined_filter_functions[idx];
			that.table.setFilterFun(fn);
		}
	);

	var option = $('<option value="">Nodes of every type</option>');
	select.append(option);

	var index = 0;
	$(predefined_filter_types).each(
		function() {
			var option = $('<option value="' + (index++) + '">' + this + '</option>');
			select.append(option);
		}
	);

	this.table.filter_input.css("width", "59%");
	this.table.filter_input.after(select);
	this.parent.append(this.table.html);

	// add link for json representation of selected nodes
	var jsonLink = $('<a href="#" title="Opens a new window containing the selected NodeUrns as JSON">Get JSON representation</a>');
	jsonLink.click(function(e) {
		e.preventDefault();
		var obj = {
			"nodeUrns" : $.map(that.table.getSelectedRows(), function(val,i) { return val.id; })
		};
		var json = JSON.stringify(obj);
		var w = window.open();
		$(w.document.body).html(json);
	});
	this.parent.append(jsonLink);
};

WiseGuiNodeTable.prototype.getSelectedNodes = function () {
	var ids = [];
	$(this.table.getSelectedRows()).each(function() {
		ids.push(this.id);
	});
	return ids;
};

WiseGuiNodeTable.prototype.setSelectedNodes = function (nodeUrnArr) {
	this.table.setSelectedRows(function(rowData) {
		return _.contains(nodeUrnArr, rowData.id);
	});
};

WiseGuiNodeTable.prototype.applyFilter = function (fn) {
	this.table.setFilterFun(fn);
};

WiseGuiNodeTable.prototype.applySelected = function (fn) {
	this.table.setSelectFun(fn);
};

module.exports = WiseGuiNodeTable;
},{"./wisegui-events.js":16,"./wisegui-node-status-icon.js":23,"./wisegui-table.js":36}],25:[function(require,module,exports){
var WiseGuiNodeTable = require('./wisegui-node-table.js');
var WiseGuiNodeSelectionSaveButton = require('./wisegui-nodeselection-save-button.js');
var WiseGuiNodeSelectionLoadButton = require('./wisegui-nodeselection-load-button.js');

/**
 * #################################################################
 * WiseGuiNodeSelectionDialog
 * #################################################################
 */

var WiseGuiNodeSelectionDialog = function(experimentId, headerHtml, bodyHtml, preSelected, storageKeyPrefix) {

	this.experimentId = experimentId;
	this.table = null;

	this.preSelected = preSelected;
	this.storageKeyPrefix = storageKeyPrefix;

	this.dialogDivId = 'WiseGuiNodeSelectionDialog-' + Math.random();

	this.dialogDiv = $('<div id="'+this.dialogDivId+'" class="modal hide WiseGuiNodeSelectionDialog"></div>');

	var bodyHeader = $(
		'<div class="modal-header">' +
		'	<h3>' + headerHtml + '</h3>' +
		'</div>'
	);

	var body = $(
		'<div class="modal-body">' +
		'	<p>' + bodyHtml + '</p>' +
		'</div>'
	);

	var imgAjaxLoader = $('<img class="ajax-loader" width="32" height="32"/>');
	imgAjaxLoader.attr("src", "img/ajax-loader-big.gif");
	body.append(imgAjaxLoader);

	var bodyFooter = $(
		'<div class="modal-footer">' +
		'	<a class="modal-cancel btn">Cancel</a>' +
		'	<a class="modal-ok btn btn-primary">OK</a>' +
		'</div>'
	);

	this.dialogDiv.append(bodyHeader, body, bodyFooter);

	this.nodeUrns = undefined;
	this.callbackCancel = undefined;
	this.callbackOK = undefined;
	this.callbacksReady = [];

	var self = this;
	wisebed.getWiseMLAsJSON(
			this.experimentId,
			function(wiseML) {
				self.constructDialogInternal(wiseML);
				self.callbacksReady.forEach(function(callback) { callback(); });
			},
			function(jqXHR, status, error) {
				self.hide();
				WiseGui.showAjaxError(jqXHR, status, error);
			}
	);
};

WiseGuiNodeSelectionDialog.prototype.setSelection = function(nodeUrns) {
	if (nodeUrns !== null && nodeUrns.length > 0) {
		window.localStorage.setItem(this.storageKeyPrefix + this.experimentId, nodeUrns.join(","));
		this.table.applySelected(function(data) { return nodeUrns.indexOf(data.id) > -1; });
	} else {
		window.localStorage.removeItem(this.storageKeyPrefix + this.experimentId);
		this.table.applySelected(function() { return false; });
	}
};

WiseGuiNodeSelectionDialog.prototype.getSelection = function() {
	var nodeUrnsString = window.localStorage.getItem(this.storageKeyPrefix + this.experimentId);
	return nodeUrnsString === null ? [] : nodeUrnsString.split(",");
};

WiseGuiNodeSelectionDialog.prototype.areSomeSelected = function() {
	return this.getSelection().length > 0;
};

WiseGuiNodeSelectionDialog.prototype.areAllSelected = function() {
	return this.getSelection().length == this.nodeUrns.length;
};

WiseGuiNodeSelectionDialog.prototype.constructDialogInternal = function(wiseML) {

	this.nodeUrns = wiseML.setup.node.map(function(node) { return node.id; });

	this.dialogDiv.on('hide', function() {
		if (this.callbackCancel) {
			this.callbackCancel();
		}
	});

	this.dialogDiv.find('.ajax-loader').attr('hidden', 'true');
	this.table = new WiseGuiNodeTable(wiseML, this.dialogDiv.find('.modal-body').first(), true, true);

	// Apply preselected
	if(typeof(this.preSelected) == "function") {
		this.table.applySelected(this.preSelected);
	} else if (this.storageKeyPrefix) {
		this.setSelection(this.getSelection());
	}

	this.nodeSelectionSaveButton = new WiseGuiNodeSelectionSaveButton(this.table.getSelectedNodes.bind(this.table));
	this.nodeSelectionLoadButton = new WiseGuiNodeSelectionLoadButton(this.table.setSelectedNodes.bind(this.table));
	
	this.dialogDiv.find('.modal-footer').prepend(this.nodeSelectionSaveButton.view);
	this.dialogDiv.find('.modal-footer').prepend(this.nodeSelectionLoadButton.view);

	// Cancel clicked
	this.dialogDiv.find('.modal-cancel').first().bind(
			'click',
			{dialog : this},
			function(event) {
				// reset to last selection set
				event.data.dialog.setSelection(event.data.dialog.getSelection());
				event.data.dialog.dialogDiv.modal('hide');
				if (event.data.dialog.callbackCancel) {
					event.data.dialog.callbackCancel();
				}
			}
	);

	// OK clicked
	this.dialogDiv.find('.modal-ok').first().bind(
			'click',
			{dialog : this},
			function(event) {
				var dialog = event.data.dialog;
				var selectedNodes = dialog.table.getSelectedNodes();
				dialog.dialogDiv.modal('hide');
				if (dialog.storageKeyPrefix) {
					dialog.setSelection(selectedNodes);
				}
				if (event.data.dialog.callbackOK) {
					event.data.dialog.callbackOK(selectedNodes);
				}
			}
	);

	if (!document.body.contains(this.dialogDiv)) {
		$(document.body).append(this.dialogDiv);
	}
};

WiseGuiNodeSelectionDialog.prototype.hide = function() {
	this.dialogDiv.modal('hide');
};

WiseGuiNodeSelectionDialog.prototype.show = function(callbackOK, callbackCancel) {
	this.callbackOK = callbackOK;
	this.callbackCancel = callbackCancel;
	this.dialogDiv.modal('show');
};

WiseGuiNodeSelectionDialog.prototype.onReady = function(callback) {
	this.callbacksReady.push(callback);
};

module.exports = WiseGuiNodeSelectionDialog;
},{"./wisegui-node-table.js":24,"./wisegui-nodeselection-load-button.js":26,"./wisegui-nodeselection-save-button.js":27}],26:[function(require,module,exports){
var WiseGuiEvents                = require('./wisegui-events.js');
var WiseGuiOptionSelectionDialog = require('./wisegui-option-selection-dialog.js');

/**
 * #################################################################
 * WiseGuiNodeSelectionLoadButton
 * #################################################################
 */

var WiseGuiNodeSelectionLoadButton = function(selectionCallback) {
	
	var self = this;
	
	this.view = $('<button class="btn">Load Selection</button>');
	this.view.bind('click', this, function(e) {
		if (!self.view.attr('disabled')) {
			var nodeSelections = JSON.parse(window.localStorage.getItem("wisegui.nodeselections")) || {};
			var callback = function(selected) {
				selectionCallback(nodeSelections[selected]);
			};
			var dialogOptions = {
				'title' : 'Pick a Node Selection',
				'selectButtonLabel' : 'Load'
			};
			new WiseGuiOptionSelectionDialog(nodeSelections, callback, undefined, dialogOptions).show();
		}
	});
	
	this.enable = function(enable) {
		if (enable) {
			self.view.removeAttr('disabled');
		} else {
			self.view.attr('disabled', 'disabled');
		}
	};

	this.updateEnabledState = function() {
		var savedSelection = window.localStorage.getItem("wisegui.nodeselections");
		this.enable(savedSelection !== null && savedSelection !== undefined);
	};

	$(window).bind(WiseGuiEvents.EVENT_NODESELECTIONS_STORAGE_CHANGED, function (e) {
		self.updateEnabledState();
	});

	this.updateEnabledState();
};

module.exports = WiseGuiNodeSelectionLoadButton;

},{"./wisegui-events.js":16,"./wisegui-option-selection-dialog.js":29}],27:[function(require,module,exports){
var WiseGuiOptionSelectionDialog = require('./wisegui-option-selection-dialog.js');
var WiseGuiEvents = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiNodeSelectionSaveButton
 * #################################################################
 */

var WiseGuiNodeSelectionSaveButton = function(getSelectionCallback) {
	this.view = $('<button class="btn">Save Selection</button>');
	this.view.bind('click', this, function(e) {
		var selection = getSelectionCallback();
		var storedNodeSelections = JSON.parse(window.localStorage.getItem("wisegui.nodeselections")) || {};
		var callback = function(name) {
			storedNodeSelections[name] = selection;
			window.localStorage.setItem("wisegui.nodeselections", JSON.stringify(storedNodeSelections));
			$(window).trigger(WiseGuiEvents.EVENT_NODESELECTIONS_STORAGE_CHANGED);
		};
		var dialogOptions = {
			'title' : 'Pick a name',
			'selectButtonLabel' : 'Save',
			'textbox' : true
		};
		new WiseGuiOptionSelectionDialog(storedNodeSelections, callback, undefined, dialogOptions).show();
	});
};

module.exports = WiseGuiNodeSelectionSaveButton;
},{"./wisegui-events.js":16,"./wisegui-option-selection-dialog.js":29}],28:[function(require,module,exports){
/**
 * #################################################################
 * WiseGuiOperationProgressView
 * #################################################################
 */

var WiseGuiOperationProgressView = function(nodeUrns, operationMaxValue, successMessage) {

	this.view = $('<div class="WiseGuiOperationProgressView"/>');
	this.successMessage = successMessage;
	this.visible = false;

	this.contents = {};

	for (var i=0; i<nodeUrns.length; i++) {

		var row = $(
			'<table>' +
			'	<tr>' +
			'	<td class="span1 nodUrnTd">'+nodeUrns[i]+'</td>' +
			'	<td class="span4 progressTd"><progress value="0" min="0" max="'+operationMaxValue+'"/></td>' +
			'	<td class="span1 statusTd"></td>' +
			'	<td class="span6 messageTd"></td>' +
			'	</tr>' +
			'</table>'
		);

		this.contents[nodeUrns[i]] = {
			row         : row,
			progressBar : row.find('progress').first(),
			statusTd    : row.find('.statusTd').first(),
			messageTd   : row.find('.messageTd').first()
		};

		this.view.append(row);
	}
};

WiseGuiOperationProgressView.prototype.update = function(operationStatus) {

	var self = this;
	var allSuccessful = true;

	$.each(operationStatus, function(nodeUrn, nodeStatus) {
		var content = self.contents[nodeUrn];
		if (content) {
			if (nodeStatus.status == 'SUCCESS') {
				content.row.remove();
				delete self.contents[nodeUrn];
			} else {
				content.progressBar[0].value = nodeStatus.statusCode;
				content.statusTd.html(nodeStatus.status);
				content.messageTd.html(nodeStatus.message);
				allSuccessful = false;
			}
		}
	});

	if (allSuccessful && this.successMessage) {
		self.view.parent().remove();
		WiseGui.showSuccessAlert(this.successMessage);
	}

	if (!allSuccessful && !this.visible) {
		WiseGui.showInfoBlockAlert(this.view);
		this.visible = true;
	}
};

module.exports = WiseGuiOperationProgressView;
},{}],29:[function(require,module,exports){
var WiseGuiOptionSelectionDialog = function(options, selectedCallback, cancelledCallback, dialogOptions) {

	var self = this;

	dialogOptions.title = dialogOptions.title || 'Please Select';
	dialogOptions.cancelButtonLabel = dialogOptions.cancelButtonLabel || 'Cancel';
	dialogOptions.selectButtonLabel = dialogOptions.selectButtonLabel || 'Select';
	dialogOptions.textbox = dialogOptions.textbox || false;
	
	this.cancelled = false;
	this.selected = undefined;

	// set up modal dialog
	var dialog = $(
		'<div class="modal hide wisegui-option-selection-dialog" id="myModal">' +
		'	<div class="modal-header">' +
		'		<button type="button" class="close" data-dismiss="modal">×</button>' +
		'		<h3>' + dialogOptions.title +'</h3>' +
		'	</div>' +
		'	<div class="modal-body">' +
		'		<form><fieldset></fieldset></form>' +
		'	</div>' +
		'	<div class="modal-footer">' +
		'		<a href="#" class="btn btn-cancel" data-dismiss="modal">' + dialogOptions.cancelButtonLabel + '</a>' +
		'		<a href="#" class="btn btn-primary btn-select" disabled="disabled">' + dialogOptions.selectButtonLabel + '</a>' +
		'	</div>' +
		'</div>');
	dialog.modal({
		backdrop : 'static'
	});
	// make dialog show on top of other "first-level" modal dialogs
	dialog.addClass('modalmodal');
	dialog.prev('div.modal-backdrop').addClass('modalmodal');

	// set up modal dialog content
	var form = dialog.find('div.modal-body fieldset');
	var loadButton = dialog.find('a.btn-select');
	var cancelButton = dialog.find('a.btn-cancel');
	
	var select = $('<select>').append('<option/>');
	for (var name in options) {
		select.append($("<option></option>").attr("value", name).text(name));
	}
	var selectLabel = $('<label class="control-label" for="'+textId+'">Choose an existing</label>');
	form.append($('<div class="control-group"/>').append(selectLabel, $('<div class="controls"/>').append(select)));

	select.change(function() {
		var selectedOption = select.find('option:selected');
		if (selectedOption.exists() && selectedOption.attr('value')) {
			loadButton.removeAttr('disabled');
			self.selected = selectedOption.attr('value');
		} else {
			loadButton.attr('disabled', 'disabled');
			self.selected = undefined;
		}
	});
	select.trigger('change');

	// set up textbox for creating a "new selection" if configured
	if (dialogOptions.textbox) {
		var textId = 'wisegui_option_selection_dialog_text_' + Math.floor(Math.random() * 100000);
		var text = $('<input type="text" id="'+textId+'"/>');
		text.on('keyup', function(e) {
			
			if(e.keyCode == 13) { // enter

				e.preventDefault();
				loadButton.click();

			} else {

				if (text.val()) {
					select.attr('disabled','disabled');
					text.parent('control-group').addClass('success');
					select.parent('control-group').addClass('error');
					loadButton.removeAttr('disabled');
					self.selected = text.val();
				} else {
					select.removeAttr('disabled');
					select.parent('control-group').removeClass('error');
					text.parent('control-group').removeClass('success');
					loadButton.attr('disabled','disabled');
					self.selected = undefined;
				}
			}
		});
		var textLabel = $('<label class="control-label" for="'+textId+'">... or pick a new one</label>');
		form.append($('<div class="control-group"/>').append(textLabel, $('<div class="controls"/>').append(text)));
	}

	// configure button actions
	loadButton.on('click', function(e) {
		e.preventDefault();
		if (!loadButton.attr('disabled')) {
			selectedCallback(self.selected);
			dialog.modal('hide');
			dialog.remove();
		}
	});

	if (cancelledCallback) {
		this.cancelled = true;
		cancelButton.on('click', cancelledCallback);
		dialog.on('hidden', function() {
			if (self.cancelled) {
				cancelledCallback();
			}
		});
	}

	dialog.on('hidden', function() {
		dialog.remove();
	});

	this.show = function() {
		dialog.modal('show');
	};
};

module.exports = WiseGuiOptionSelectionDialog;
},{}],30:[function(require,module,exports){
var WiseGuiNodeTable               = require('./wisegui-node-table.js');
var WiseGuiGoogleMapsView          = require('./wisegui-google-maps-view.js');
var WiseGuiNodeSelectionSaveButton = require('./wisegui-nodeselection-save-button.js');
var WiseGuiNodeSelectionLoadButton = require('./wisegui-nodeselection-load-button.js');
var WiseGuiEvents                  = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiReservationDialog
 * #################################################################
 */

var WiseGuiReservationDialog = function() {
	this.table = null;
	this.view = $('<div id="WiseGuiReservationDialog" class="modal hide reservation"></div>');
	$(document.body).append(this.view);
	this.buildView();
	this.show();
};

WiseGuiReservationDialog.prototype.hide = function() {
	this.view.modal('hide');
};

WiseGuiReservationDialog.prototype.show = function() {
	this.view.modal('show');
};

WiseGuiReservationDialog.prototype.buildView = function() {

	Array.prototype.diff = function(a) {
	    return this.filter(function(i) { return a.indexOf(i) <= -1; });
	};

	var that = this;

	var dialogHeader = $('<div class="modal-header"><h3>Make a reservation</h3></div>');

	var now = new Date();
	now.setSeconds(0);

	var format = function(val) {
		// Prepend a zero
		if(val >=0 && val <= 9) {
			return "0" + val;
		} else {
			return val;
		}
	};

	var yyyy = now.getFullYear();
	var mm = (now.getMonth());
	var dd = now.getDate();
	var ii = now.getMinutes();
	var hh = now.getHours();

	// Hint: it works even over years
	var in_one_hour = new Date(yyyy,mm,dd,hh+1,ii,0);

	var date_start = format(dd) + "." + format(mm+1) + "." + yyyy;
	var time_start = format(hh) + ":" + format(ii);

	var date_end = format(in_one_hour.getDate()) + "." + format(in_one_hour.getMonth() +1) + "." + in_one_hour.getFullYear();
	var time_end = format(in_one_hour.getHours()) + ":" + format(in_one_hour.getMinutes());

	
	// Create the inputs
	var input_date_start = $('<input type="text" value="' + date_start + '" id="input_date_start" style="width:75px"/>');
	var input_time_start = $('<input type="text" value="' + time_start + '" id="input_time_start" style="width:40px"/>');
	var input_date_end =   $('<input type="text" value="' + date_end + '" id="input_date_end" style="width:75px"/>');
	var input_time_end =   $('<input type="text" value="' + time_end + '" id="input_time_end" style="width:40px"/>');
	var input_desciption = $('<input type="text" id="description" style="width:280px"/>');

	var p_nodes = $("<p></p>");

	var tabs = $(
			'<ul class="nav nav-tabs">' +
			'	<li class="active"><a href="#WiseGuiTestbedMakeReservationList" data-toggle="tab">List</a></li>' +
			'	<li><a href="#WiseGuiTestbedMakeReservationMap" data-toggle="tab">Map</a></li>' +
			'</ul>' +
			'<div class="tab-content">' +
			'	<div class="tab-pane active" id="WiseGuiTestbedMakeReservationList"></div>' +
			'	<div class="tab-pane" id="WiseGuiTestbedMakeReservationMap"></div>' +
			'</div>'
	);
	
	tabs.find('#WiseGuiTestbedMakeReservationList').append(p_nodes);

	tabs.find('a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	var showTable = function (wiseML) {
		that.table = new WiseGuiNodeTable(wiseML, p_nodes, true, true);
	};
	
	var createMap = function() {
		wisebed.getWiseMLAsJSON(null, showMap,
				function(jqXHR, textStatus, errorThrown) {
					console.log('TODO handle error in WiseGuiReservationDialog');
					WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
				}
		);
	};

	wisebed.getWiseMLAsJSON(null, showTable,
			function(jqXHR, textStatus, errorThrown) {
				console.log('TODO handle error in WiseGuiReservationDialog');
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}, createMap
	);

	 //Show specialized google map for reservation 
	var showMap = function(wiseML){
		
		that.mapsView = new WiseGuiGoogleMapsView(wiseML, tabs.find('#WiseGuiTestbedMakeReservationMap'));
		var mapsView = that.mapsView;

		if (mapsView.map !== null) {

			mapsView.map.enableKeyDragZoom();
			mapsView.map.selectedURNs = [];
			
			var ICON_SELECTED   = 'img/maps/yellow-dot.png';
			var ICON_DESELECTED = 'img/maps/red-dot.png';
			
			$.each(mapsView.markersArray, function (index, marker) {

				mapsView.mapSpiderfier.clearListeners('click');
				mapsView.mapSpiderfier.addListener('click', function(marker, event) {

					if (marker.getIcon() == ICON_SELECTED){
						mapsView.map.selectURNs([marker.urn], true);
					} else {
						mapsView.map.selectURNs([marker.urn], false);
					}

					var selectedFun = function(data) {
						var nodeids = mapsView.map.selectedURNs;
						for(var i = 0; i < nodeids.length; i++) {
							if(data.id == nodeids[i]) return true;
						}
						return false;
					};

					that.table.applySelected(selectedFun);
				});
	        });
			
			mapsView.map.selectURNs = function(urns, deselect){
				
				//loop over markers and set new image
				$.each(mapsView.markersArray, function (index, marker) {
					if (urns.indexOf(marker.urn) > -1) {
						marker.setIcon(deselect ? ICON_DESELECTED : ICON_SELECTED);
					}
	            });
				
				//loop over selected urns and insert them to model if necessary
				$.each(urns, function(idx, urn) {
					var index = mapsView.map.selectedURNs.indexOf(urn);
					if(index == -1) {
						if (!deselect) {
							mapsView.map.selectedURNs.push(urn);
						} else {
							mapsView.map.selectedURNs.splice(index,1);
						}
					} else {
						if (deselect) {
							mapsView.map.selectedURNs.splice(index,1);
						}
					}	
				});
			};

			//Filter out all markers but the ones with the urns given
			mapsView.map.filter = function(urns){
				//loop over markers and set new image
				var markersArray = mapsView.markersArray;
				$.each(markersArray, function (index, marker) {
					if(urns.indexOf(marker.urn)==-1){
			            marker.setMap(null);
					}else{
						marker.setMap(mapsView.map);
					}
			    });
			};
			
			that.table.table.addSelectionListener(mapsView.map.selectURNs);
			that.table.table.addFilterListener(mapsView.map.filter);
			
			var dz = mapsView.map.getDragZoomObject();
			
			google.maps.event.addListener(dz, 'dragend', function(bounds) {       
	            var selectedURNs = [];
	            var deselectedURNs = [];
	            
	            // get nodes that have been (de-)selected
	            mapsView.markersArray.forEach(function(marker) {
	            	if (bounds.contains(marker.getPosition()) && marker.getMap() !== null) {
	                    if (marker.getIcon() == ICON_DESELECTED) {
	                    	selectedURNs.push(marker.urn);
	                    } else {
	                    	deselectedURNs.push(marker.urn);
	                    }
	                }
	            });
	            
	            // update the map
	            mapsView.map.selectURNs(selectedURNs);
	            mapsView.map.selectURNs(deselectedURNs, true);
	            
	            //remove all deselected nodes
	            mapsView.map.selectedURNs = mapsView.map.selectedURNs.diff(deselectedURNs);
	            //update selection in table
	            var selectedFun =function(data) {
					var nodeids = mapsView.map.selectedURNs;
					for(var i = 0; i < nodeids.length; i++) {
						if(data.id == nodeids[i]) return true;
					}
					return false;
				};
	            
	           that.table.applySelected(selectedFun);
	    	});
			
			tabs.find('li a[href=#WiseGuiTestbedMakeReservationMap]').on('shown', function(e) {
				google.maps.event.trigger(that.mapsView.map, 'resize');
				that.mapsView.setBounds();
			});
		}
	};

	tabs.find('#WiseGuiTestbedMakeReservationMap').append("<h4>Click on single nodes or Shift+Click for bounding box</h4>");

	// Add the picker
    input_date_start.datepicker({dateFormat: 'dd.mm.yy'});
    input_date_end.datepicker({dateFormat: 'dd.mm.yy'});
    input_time_start.timePicker({step: 5});
    input_time_end.timePicker({step: 5});

    var h4_nodes = $("<h4>Select the nodes to reserve</h4>");

    var onError = function (msg) {

		dialogBody.prepend($(
			'<div class="alert alert-error">' +
			'	<a class="close" data-dismiss="alert" href="#">&times;</a>' +
			' <p>'+msg+'</p>' +
			'</div>'
		));

		okButton.removeAttr("disabled");
		cancelButton.removeAttr("disabled");
    };

    var span_start = $('<span>Start: </span>');
    var span_end = $('<span style="margin-left:10px;">End: </span>');
    var span_description = $('<span style="margin-left:10px;">Description: </span>');

	var dialogBody = $('<div class="modal-body reservation-body"/></div>');
	dialogBody.append(span_start, input_date_start, input_time_start);
	dialogBody.append(span_end, input_date_end, input_time_end);
	dialogBody.append(span_description, input_desciption);
	
	dialogBody.append(h4_nodes);
	dialogBody.append(tabs);

	
	var okButton = $('<input class="btn btn-primary" value="Reserve" style="width:50px;text-align:center;">');
	var cancelButton = $('<input class="btn" value="Cancel" style="width:45px;text-align:center;">');

	okButton.bind('click', this, function(e) {

		okButton.attr("disabled", "true");
		cancelButton.attr("disabled", "true");

		input_date_start.removeClass("error");
		input_date_end.removeClass("error");
		input_time_start.removeClass("error");
		input_time_end.removeClass("error");

		var dateStart = explode(".", input_date_start.val());
		var dateEnd = explode(".", input_date_end.val());

		var timeStart = explode(":", input_time_start.val());
		var timeEnd = explode(":", input_time_end.val());

		var nodes = that.table.getSelectedNodes();

		if(dateStart.length != 3) {
			input_date_start.addClass("error");
			onError("Start date incorrect.");
			return;
		} else if (dateEnd.length != 3) {
			input_date_end.addClass("error");
			onError("End date incorrect.");
			return;
		} else if (timeStart.length != 2) {
			input_time_start.addClass("error");
			onError("Start time incorrect.");
			return;
		} else if (timeEnd.length != 2){
			input_time_end.addClass("error");
			onError("End time incorrect.");
			return;
		}

		var from = new Date(dateStart[2], dateStart[1]-1, dateStart[0], timeStart[0], timeStart[1], 0);
		var to = new Date(dateEnd[2], dateEnd[1]-1, dateEnd[0], timeEnd[0], timeEnd[1], 0);

		if(to <= from) {
			input_date_start.addClass("error");
			input_date_end.addClass("error");
			input_time_start.addClass("error");
			input_time_end.addClass("error");
			onError("End date must after the start date.");
			return;
		} else if(nodes.length <= 0) {
			onError("You must select at least one node");
			return;
		}

		var callbackError = function(jqXHR, textStatus, errorThrown) {
			onError(jqXHR.responseText);
		};

		var callbackDone = function() {

			okButton.removeAttr("disabled");
			cancelButton.removeAttr("disabled");

			that.hide();

			// Refresh the reservation table
			$(window).trigger(WiseGuiEvents.EVENT_RESERVATIONS_CHANGED);

			// Refresh the experiments tab in the menu
			$(window).trigger(WiseGuiEvents.EVENT_NAVIGATION, getNavigationData());
		};

		// TODO support key-value pairs in "options" field
		wisebed.reservations.make(
			from,
			to,
			nodes,
			input_desciption.val(),
			[],
			callbackDone,
		    callbackError
		);
	});

	cancelButton.bind('click', this, function(e) {
		e.data.hide();
	});

	var nodeSelectionSaveButton = new WiseGuiNodeSelectionSaveButton(function() {
		return that.table.getSelectedNodes();
	});

	var nodeSelectionLoadButton = new WiseGuiNodeSelectionLoadButton(function(selection) {
		that.table.setSelectedNodes(selection);
	});

	var dialogFooter = $('<div class="modal-footer"/>');
	dialogFooter.append(nodeSelectionLoadButton.view, nodeSelectionSaveButton.view, okButton, cancelButton);
	this.view.append(dialogHeader, dialogBody, dialogFooter);
};

module.exports = WiseGuiReservationDialog;
},{"./wisegui-events.js":16,"./wisegui-google-maps-view.js":18,"./wisegui-node-table.js":24,"./wisegui-nodeselection-load-button.js":26,"./wisegui-nodeselection-save-button.js":27}],31:[function(require,module,exports){
var WiseGuiEvents = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiReservationObserver
 * #################################################################
 */

var WiseGuiReservationObserver = function() {
	this.lastKnownReservations = [];
	this.isObserving           = false;
	this.schedule              = undefined;
};

WiseGuiReservationObserver.prototype.fetchReservationsAndProcess = function() {
	var self = this;
	console.log("WiseGuiReservationObserver fetching personal reservations");
	wisebed.reservations.getPersonal(
			null,
			null,
			function(reservations) {self.processReservationsFetched(reservations);},
			WiseGui.showAjaxError
	);
};

WiseGuiReservationObserver.prototype.processReservationsFetched = function(reservations) {

	var newReservations = [];

	for (var i=0; i<reservations.length; i++) {

		var knownReservation = false;

		for (var j=0; j<this.lastKnownReservations.length; j++) {
			if (wisebed.reservations.equals(reservations[i], this.lastKnownReservations[j])) {
				knownReservation = true;
				break;
			}
		}

		if (!knownReservation) {
			newReservations.push(reservations[i]);
		}
	}

	for (var k=0; k<newReservations.length; k++) {
		$(window).trigger(WiseGuiEvents.EVENT_RESERVATION_ADDED, newReservations[k]);
		this.lastKnownReservations.push(newReservations[k]);
	}
};

WiseGuiReservationObserver.prototype.startObserving = function() {

	var self = this;

	$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function(e, data) {
		self.schedule = window.setInterval(function() {self.fetchReservationsAndProcess();}, 60 * 1000);
		self.fetchReservationsAndProcess();
		console.log('WiseGuiReservationObserver beginning to observe reservations');
	});

	$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function(e, data) {
		self.stopObserving();
	});
};

WiseGuiReservationObserver.prototype.stopObserving = function() {

	console.log('WiseGuiReservationObserver.stopObserving()');

	if (this.schedule !== undefined) {
		window.clearInterval(this.schedule);
		this.schedule = undefined;
		console.log('WiseGuiReservationObserver stopped to observe');
	}
};

module.exports = WiseGuiReservationObserver;

},{"./wisegui-events.js":16}],32:[function(require,module,exports){
var WiseGuiConsoleView   = require('./wisegui-console-view.js');
var WiseGuiFlashView     = require('./wisegui-flash-view.js');
var WiseGuiResetView     = require('./wisegui-reset-view.js');
var WiseGuiSendView      = require('./wisegui-send-view.js');
var WiseGuiScriptingView = require('./wisegui-scripting-view.js');
var WiseGuiEvents        = require('./wisegui-events.js');

/**
 * #################################################################
 * WiseGuiReservationView
 * #################################################################
 */

var WiseGuiReservationView = function(reservation) {

	this.reservation             = reservation;
	this.experimentId            = reservation.experimentId;

	this.reservationViewDivId    = 'WiseGuiReservationView-'+this.experimentId.replace(/=/g, '');
	this.outputsTextAreaId       = this.reservationViewDivId+'-outputs-textarea';
	this.sendDivId               = this.reservationViewDivId+'-send';
	this.flashDivId              = this.reservationViewDivId+'-flash';
	this.resetDivId              = this.reservationViewDivId+'-reset';
	this.scriptingEditorDivId    = this.reservationViewDivId+'-scripting-editor';
	this.scriptingOutputDivId    = this.reservationViewDivId+'-scripting-output';
	this.wisemlJsonDivId         = this.reservationViewDivId+'-wiseml-json';
	this.wisemlXmlDivId          = this.reservationViewDivId+'-wiseml-xml';

	this.view = $('<div class="WiseGuiReservationView"/>');

	this.buildView();
	this.loadWisemlViews();
};

WiseGuiReservationView.prototype.loadWisemlViews = function() {
	var self = this;
	wisebed.getWiseMLAsJSON(this.experimentId, function(wiseML) {
		var jsonTab = $('#' + self.wisemlJsonDivId);
		jsonTab.append($('<pre class="WiseGuiReservationViewWiseMLJSON">'+JSON.stringify(wiseML, wiseMLNullFilter, '  ')+'</pre>'));
		jsonTab.append($('<a href="'+wisebedBaseUrl + '/experiments/'+self.experimentId+'/network.json" target="_blank" class="btn btn-primary pull-right">Download</a>'));
	}, WiseGui.showAjaxError);
	wisebed.getWiseMLAsXML(this.experimentId, function(wiseML) {
		var xmlTab = $('#' + self.wisemlXmlDivId);
		xmlTab.append($('<pre class="WiseGuiReservationViewWiseMLXML">'+wiseML.replace(/</g,"&lt;")+'</pre>'));
		xmlTab.append($('<a href="'+wisebedBaseUrl + '/experiments/'+self.experimentId+'/network.xml" target="_blank" class="btn btn-primary pull-right">Download</a>'));
	}, WiseGui.showAjaxError);
};

WiseGuiReservationView.prototype.buildView = function() {
	
	this.consoleView = new WiseGuiConsoleView(this.reservation);
	this.view.append(this.consoleView.view);

	this.view.append(
			' <div class="WiseGuiReservationViewControls"><h2>Controls</h2></div>' +
			'	 <div>' +
			'		<ul class="nav nav-tabs">' +
			'			<li class="active"    ><a href="#'+this.flashDivId+'">Flash</a></li>' +
			'			<li                   ><a href="#'+this.resetDivId+'">Reset</a></li>' +
			'			<li                   ><a href="#'+this.sendDivId+'">Send Message</a></li>' +
			'			<li                   ><a href="#'+this.scriptingEditorDivId+'">Scripting Editor</a></li>' +
			'			<li                   ><a href="#'+this.scriptingOutputDivId+'">Scripting Output</a></li>' +
			'			<li class="pull-right"><a href="#'+this.wisemlXmlDivId+'">WiseML (XML)</a></li>' +
			'			<li class="pull-right"><a href="#'+this.wisemlJsonDivId+'">WiseML (JSON)</a></li>' +
			'		</ul>' +
			'		<div class="tab-content">' +
			'			<div class="active tab-pane WiseGuiConsoleViewFlashControl" id="'+this.flashDivId+'"></div>' +
			'			<div class="       tab-pane WiseGuiConsoleViewResetControl" id="'+this.resetDivId+'"></div>' +
			'			<div class="       tab-pane WiseGuiConsoleViewSendControl" id="'+this.sendDivId+'"/>' +
			'			<div class="       tab-pane WiseGuiConsoleViewScriptingControl" id="'+this.scriptingEditorDivId+'"/>' +
			'			<div class="       tab-pane WiseGuiConsoleViewScriptingOutputTab" id="'+this.scriptingOutputDivId+'"/>' +
			'			<div class="       tab-pane WiseGuiConsoleViewWisemlXmlTab" id="'+this.wisemlXmlDivId+'"/>' +
			'			<div class="       tab-pane WiseGuiConsoleViewWisemlJsonTab" id="'+this.wisemlJsonDivId+'"/>' +
			'		</div>' +
			'	</div>' +
			'</div>'
	);
	
	// attach views
	this.flashView = new WiseGuiFlashView(this.reservation);
	this.view.find('#'+this.flashDivId).append(this.flashView.view);

	this.resetView = new WiseGuiResetView(this.reservation);
	this.view.find('#'+this.resetDivId).append(this.resetView.view);

	this.sendView = new WiseGuiSendView(this.reservation);
	this.view.find('#'+this.sendDivId).append(this.sendView.view);

	//this.channelPipelinesView = new WiseGuiChannelPipelinesView(this.reservation);
	//this.view.find('#'+this.channelPipelinesDivId).append(this.channelPipelinesView.view);

	this.scriptingView = new WiseGuiScriptingView(this.reservation);
	this.view.find('#'+this.scriptingEditorDivId).append(this.scriptingView.editorView);
	this.view.find('#'+this.scriptingOutputDivId).append(this.scriptingView.outputView);

	// bind some actions

	var self = this;
	var tabs = this.view.find('.nav-tabs').first();

	tabs.find('a').click(function (e) {
		e.preventDefault();
		var navigationData = getNavigationData();
		navigationData.tab = e.target.hash.substring(1);
		window.location.hash = $.param(navigationData);
	});

	$(window).bind(WiseGuiEvents.EVENT_NAVIGATION, function(e, navigationData) {
		if (navigationData.tab) {
			tabs.find('a[href="#'+navigationData.tab+'"]').tab('show');
		}
	});
};

module.exports = WiseGuiReservationView;
},{"./wisegui-console-view.js":15,"./wisegui-events.js":16,"./wisegui-flash-view.js":17,"./wisegui-reset-view.js":33,"./wisegui-scripting-view.js":34,"./wisegui-send-view.js":35}],33:[function(require,module,exports){
var WiseGuiNodeSelectionDialog = require('./wisegui-nodeselection-dialog.js');
var WiseGuiOperationProgressView = require('./wisegui-operation-progress-view.js');

/**
 * #################################################################
 * WiseGuiResetView
 * #################################################################
 */

var WiseGuiResetView = function(reservation) {
	
	this.reservation              = reservation;
	this.experimentId             = reservation.experimentId;

	this.view                     = null;
	this.resetNodeSelectionButton = null;
	this.resetResetButton         = null;
	this.resetNodeSelectionDialog = null;

	this.resetSelectedNodeUrns    = [];

	this.buildView();
};

WiseGuiResetView.prototype.buildView = function() {

 	this.view = $(
 		'<div class="row">' +
		'	<div class="span4">' +
		'		<button class="btn WiseGuiResetViewNodeSelectionButton span4">Select Nodes</button>' +
		'	</div>' +
		'	<div class="span4">' +
		'		<button class="btn btn-primary WiseGuiResetViewResetButton span4" disabled>Reset Nodes</button>' +
		'	</div>' +
		'</div>'
	);

 	this.resetNodeSelectionButton = this.view.find('button.WiseGuiResetViewNodeSelectionButton').first();
	this.resetResetButton         = this.view.find('button.WiseGuiResetViewResetButton').first();

	this.resetNodeSelectionDialog = new WiseGuiNodeSelectionDialog(
			this.experimentId,
			'Reset Nodes',
			'Please select the nodes you want to reset.',
			this.resetSelectedNodeUrns,
			'nodeselection.reset.'
	);

	this.resetNodeSelectionButton.bind('click', this, function(e) {
		e.data.showResetNodeSelectionDialog();
	});

	this.updateResetSelectNodeUrns();

	this.resetResetButton.bind('click', this, function(e) {
		e.data.executeResetNodes();
	});

	WiseGui.bindToReservationState(this.view.find('button'), this.experimentId);
};

WiseGuiResetView.prototype.updateResetSelectNodeUrns = function() {
	this.resetSelectedNodeUrns = this.resetNodeSelectionDialog.getSelection();
	this.setResetButtonDisabled(this.resetSelectedNodeUrns.length === 0);
	this.resetNodeSelectionButton.html((this.resetSelectedNodeUrns.length == 1 ?
			'1 node selected' :
			this.resetSelectedNodeUrns.length + ' nodes selected'
	));
};

WiseGuiResetView.prototype.showResetNodeSelectionDialog = function() {

	this.setResetSelectNodesButtonDisabled(true);
	var self = this;
	wisebed.getWiseMLAsJSON(
			this.reservation.experimentId,
			function(wiseML) {

				self.setResetSelectNodesButtonDisabled(false);

				// TODO: Refactor, also used in addFlashConfiguration
				if(self.resetSelectedNodeUrns !== null && self.resetSelectedNodeUrns.length > 0) {
					preSelected = function(data) {
						var nodeids = self.resetSelectedNodeUrns;
						for(var i = 0; i < nodeids.length; i++) {
							if(data.id == nodeids[i]) return true;
						}
						return false;
					};
				}

				self.resetNodeSelectionDialog.show(function() {
					self.updateResetSelectNodeUrns();
				});

			}, function(jqXHR, textStatus, errorThrown) {
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
};

WiseGuiResetView.prototype.setResetSelectNodesButtonDisabled = function(disabled) {
	this.resetResetButton.attr('disabled', disabled);
};

WiseGuiResetView.prototype.setResetButtonDisabled = function(disabled) {
	this.resetResetButton.attr('disabled', disabled);
};

WiseGuiResetView.prototype.executeResetNodes = function() {

	this.setResetButtonDisabled(true);
	var self = this;
	wisebed.experiments.resetNodes(
			this.reservation.experimentId,
			this.resetSelectedNodeUrns,
			function(result) {
				var progressView = new WiseGuiOperationProgressView(
						self.resetSelectedNodeUrns, 1,
						"All nodes were successfully reset."
				);
				progressView.update(result);
				self.setResetButtonDisabled(false);
			},
			function(jqXHR, textStatus, errorThrown) {
				self.setResetButtonDisabled(false);
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
};

module.exports = WiseGuiResetView;
},{"./wisegui-nodeselection-dialog.js":25,"./wisegui-operation-progress-view.js":28}],34:[function(require,module,exports){
/**
 * #################################################################
 * WiseGuiScriptingView
 * #################################################################
 */

var WiseGuiScriptingView = function(reservation) {

	this.reservation     = reservation;
	this.experimentId    = reservation.experimentId;

	this.userScript      = {};

	this.editorView      = null;
	this.editorDiv       = null;
	this.editorDivId     = 'WiseGuiScriptingView-editor-'+this.experimentId;
	this.editorHelpModal = null;
	this.buildEditorView();

	this.outputView      = null;
	this.outputDiv       = null;
	this.outputDivId     = 'WiseGuiScriptingView-output-'+this.experimentId;
	this.buildOutputView();
};

WiseGuiScriptingView.prototype.buildEditorView = function() {

	this.editorView = $(
		'<div class="row" style="padding-bottom:10px;">' +
		'	<div class="span6">' +
		'		<a class="btn span2 WiseGuiScriptingViewHelpButton" href="#" data-toggle="modal" data-target="#scriptingHelpModal">Help</a>' +
		'	</div>' +
		'	<div class="span6" style="text-align:right;">' +
		'		<button class="btn btn-danger span2 WiseGuiScriptingViewStopButton">Stop</button>' +
		'		<button class="btn btn-success span2 WiseGuiScriptingViewStartButton">Start</button>' +
		'	</div>' +
		'</div>' +
		'<div class="row">' +
		'		<div class="span12 WiseGuiScriptingViewEditorRow"></div>' +
		'</div>'
	);

	// ******* start ACE displaying error workaround ********
	// ace editor is not correctly displayed if parent tab is hidden when creating it. therefore we need to workaround
	// by attaching it to the body (invisible on z-index -1), making the div an ace editor, removing the z-index and
	// moving the element in the dom to its final destination
	this.editorRow         = this.editorView.find('div.WiseGuiScriptingViewEditorRow').first();
	this.editorStopButton  = this.editorView.find('button.WiseGuiScriptingViewStopButton').first();
	this.editorStartButton = this.editorView.find('button.WiseGuiScriptingViewStartButton').first();
	this.editorHelpButton  = this.editorView.find('button.WiseGuiScriptingViewScriptingHelpButton').first();
	this.editorDiv         = $(
		'<div class="WiseGuiScriptingViewEditor" style="z-index:-1;">' +
		'WiseGuiUserScript = function() {\n' +
		'  console.log("WiseGuiUserScript instantiated...");\n' +
		'  this.experimentId = null;\n' +
		'  this.webSocket = null;\n' +
		'  this.outputDiv = null;\n' +
		'  this.outputTextArea = null;\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.start = function(env) {\n' +
		'  console.log("Starting user script...");\n' +
		'  this.experimentId = env.experimentId;\n' +
		'  this.outputDiv = env.outputDiv;\n' +
		'  this.outputDiv.empty();\n' +
		'  this.outputTextArea = $("&lt;textarea class=\'span12\' style=\'height:500px\'/>");\n' +
		'  this.outputDiv.append(this.outputTextArea);\n' +
		'  \n' +
		'  var self = this;\n' +
		'  this.webSocket = new wisebed.WebSocket(\n' +
		'      this.experimentId,\n' +
		'      function() {self.onmessage(arguments);},\n' +
		'      function() {self.onopen(arguments);},\n' +
		'      function() {self.onclosed(arguments);}\n' +
		'  );\n' +
		'  // TODO implement me\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.stop = function() {\n' +
		'  console.log("Stopping user script...");\n' +
		'  this.webSocket.close();\n' +
		'  // TODO implement me\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.onmessage = function(message) {\n' +
		'  console.log(message);\n' +
		'  this.outputTextArea.html(this.outputTextArea.html() + "\\n" + JSON.stringify(message));\n' +
		'  // TODO implement me\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.onopen = function(event) {\n' +
		'  console.log(event);\n' +
		'  this.outputTextArea.html(this.outputTextArea.html() + "\\nConnection opened!");\n' +
		'  // TODO implement me\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.onclosed = function(event) {\n' +
		'  console.log(event);\n' +
		'  this.outputTextArea.html(this.outputTextArea.html() + "\\nConnection closed!");\n' +
		'  // TODO implement me\n' +
		'};\n' +
		'</div>'
	);

	$(document.body).append(this.editorDiv);

	this.editor = ace.edit(this.editorDiv[0]);
	this.editor.setTheme("ace/theme/textmate");
	//var JavaScriptMode = require("ace/mode/javascript").Mode;
	//this.editor.getSession().setMode(new JavaScriptMode());
	this.editorDiv.attr('style', '');
	this.editorRow.append(this.editorDiv);

	// ******* end ACE displaying error workaround ********

	this.editorHelpModal =
		'<div id="scriptingHelpModal" class="modal hide">' + 
		'<div class="modal-header">' +
		'  <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' +
		'	 <h1>How to use the scripting environment?</h1>' +
		'</div>' +
		'<div class="modal-body">' +
		'The scripting environment allows the user to write arbitrary JavaScript code into the editor. This ' +
		'functionality can e.g., be used to connect to the currently running experiment via WebSockets and ' +
		'process the messages received from the sensor nodes to e.g., build visualizations or statistical ' +
		'evaluations during the runtime of the experiment while data is flowing.' +
		'' +
		'<h3>Using lifecycle callbacks</h3>' +
		'If a script conforms to a certain class name and function skeleton the scripting environment ' +
		'provides additional support for object-oriented scripting and lifecycle hooks. To get this support ' +
		'base your script on the skeleton below:' +
		'' +
		'<pre>\n' +
		'WiseGuiUserScript = function() {\n' +
		'  console.log("WiseGuiUserScript instantiated...");\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.start = function(env) {\n' +
		'  console.log("WiseGuiUserScript started, connecting to reservation...");\n' +
		'  this.onmessage = function(message) { console.log(message); }\n' +
		'  this.onopen = function(event) { console.log(event); }\n' +
		'  this.onclose = function(event) { console.log(event); }\n' +
		'  this.webSocket = new wisebed.WebSocket(\n' +
		'    env.experimentId,\n' +
		'    this.onmessage,\n' +
		'    this.onopen,\n' +
		'    this.onclose\n' +
		'  );\n' +
		'};\n' +
		'\n' +
		'WiseGuiUserScript.prototype.stop = function() {\n' +
		'  console.log("WiseGuiUserScript stopped...");\n' +
		'};\n' +
		'</pre>' +
		'' +
		'This way the environment can "start" the users script by calling ' +
		'<code>var userScript = new WiseGuiUserScriptClass(); userScript.start(env)</code> where ' +
		'<code>env</code> is a JavaScript object containing informations about the enironment (see below).<br/>' +
		'<br/>' +
		'When the user stops the script the environment will call <code>userScript.stop()</code>, remove ' +
		'the <code>&lt;script></code> tag from the DOM.<br/>' +
		'' +
		'<h3>What type of events are received over the WebSocket connection?</h3>' +
		'The testbed back end produces several types of events, an example of each listed below:<br/>' +
		'<ul>' +
		'  <li>' +
		'    <emph>Devices attached/detached</emph>: produced whenever one or more devices are attached or detached from ' +
		'    the back end. This can either be the case if a wired device gets physically or logically ' +
		'    attached/detached. A logical detach can e.g., happen if a mobile device moves out of communication' +
		'    range of a gateway or if the back end software on the gateway crashes or is shut down. Example:' +
		'<pre>' +
		'{\n' +
		'  "type":"devicesDetached",\n' +
		'  "nodeUrns":["urn:wisebed:uzl1:0x2069"],\n' +
		'  "timestamp":"2013-07-11T09:39:02.226+02:00"\n' +
		'}\n' +
		'</pre>' +
		'<pre>' +
		'{\n' +
		'  "type":"devicesAttached",\n' +
		'  "nodeUrns":["urn:wisebed:uzl1:0x2069"],\n' +
		'  "timestamp":"2013-07-11T09:39:07.558+02:00"\n' +
		'}\n' +
		'</pre>' +
		'  </li>' +
		'  <li>' +
		'    <emph>Device outputs</emph>: produced whenever a device outputs data on e.g., its serial port which' +
		'    is then forwarded as an event to the user. Example:<br/>' +
		'<pre>' +
		'{\n' +
		'  "type":"upstream",\n' +
		'  "payloadBase64":"EAJoAGlTZXJBZXJpYWxBcHAgQm9vdGluZywgaWQ9MHgyMDY5EAM=",\n' +
		'  "sourceNodeUrn":"urn:wisebed:uzl1:0x2069",\n' +
		'  "timestamp":"2013-07-11T09:39:07.589+02:00"\n' +
		'}' +
		'</pre>' +
		'  </li>' +
		'  <li>' +
		'    <emph>Reservation started/ended</emph>: produced when a reservation time stamp starts or ends. Example:<br/>' +
		'<pre>' +
		'{\n' +
		'  "type":"reservationEnded",\n' +
		'  "timestamp":"2013-07-11T08:42:00.000+02:00"\n' +
		'}\n' +
		'</pre>' +
		'<pre>' +
		'{\n' +
		'  "type":"reservationStarted",\n' +
		'  "timestamp":"2013-07-11T09:45:00.000+02:00"\n' +
		'}\n' +
		'</pre>' +
		'  </li>' +
		'</ul>' +
		'' +
		'<h3>How does the scripting environment look like?</h3>' +
		'The <code>env</code> variable that is passed to the <code>start()</code> function of the users script ' +
		'contains information about the environment/context in which the script is executed. Below you see an ' +
		'example <code>env</code> content:' +
		'' +
		'<pre>' +
		'{\n' +
		'  experimentId : ABCD1234567890EF,\n' +
		'  outputDivId  : \'WiseGuiExperimentsViewScriptingOutputTab-ABCD1234567890EF\',\n' +
		'  outputDiv    : {...}\n' +
		'}' +
		'</pre>' +
		'<code>env.experimentId</code> can e.g., be used to call the ' +
		'functions of wisebed.js to e.g., connect to the currently running experiments using WebSockets. ' +
		'<code>env.outputDivId</code> is the ID of the <code>&lt;div></code>DOM element that represents the ' +
		'"Scripting Output" tab so you can access it using <code>document.getElementById(env.outputDivId)</code> ' +
		'or using the jQuery-based variant <code>$(env.outputDivId)</code> (which is identical to ' +
		'<code>env.outputDiv</code>).<br/>' +
		'<br/>' +
		'Please note that your script may use all JavaScript libraries currently loaded in the document such ' +
		'as jQuery or Twitter Bootstrap GUI elements and scripts.' +
		'' +
		'<h3>How is the code executed?</h3>' +
		'The scripting environment takes the code from the editor and attaches it the current documents DOM ' +
		'using a <code>&lt;script></code> tag whereby the user-supplied script is automatically executed ' +
		'(i.e. evaluated). So either you include function calls to your self-written functions in your script ' +
		'or base your script on the template as described above to make sure something is actually executed.' +
		'' +
		'<h3>Please be aware...</h3>' +
		'... that there\'s no way yet to really clean up after running a user-provided ' +
		'JavaScript script. Therefore, if your script doesn\'t cleanly shut down or breaks something the only ' +
		'thing that definitely helps is to reload the browser tab to set the application back to a clean state!' +
		'</div>' +
		'</div>';

	$(document.body).append(this.editorHelpModal);
	
	var self = this;
	this.editorStopButton.attr('disabled', true);
	this.editorStartButton.bind('click', self, function(e) { self.startUserScript(); });
	this.editorStopButton.bind('click', self, function(e) { self.stopUserScript(); });

	WiseGui.bindToReservationState(this.editorStartButton, this.experimentId);
};

WiseGuiScriptingView.prototype.startUserScript = function() {

	this.stopUserScript();

	this.editorStartButton.attr('disabled', true);

	this.userScriptDomElem = document.createElement('script');
	this.userScriptDomElem.text = this.editor.getSession().getValue();
	this.userScriptDomElem.id = 'WiseGuiUserScriptDomElem-' + this.experimentId;

	document.body.appendChild(this.userScriptDomElem);

	this.editorStartButton.attr('disabled', true);
	this.editorStopButton.attr('disabled', false);

	if (typeof(WiseGuiUserScript) == 'function') {

		this.userScript = new WiseGuiUserScript();

		if (typeof(this.userScript) == 'object') {

			if ('start' in this.userScript && typeof(this.userScript.stop) == 'function') {
				this.userScript.start({
					experimentId : this.experimentId,
					outputDivId  : this.outputDivId,
					outputDiv    : this.outputDiv
				});
			}

		} else {
			alert("error");
		}
	}
};

WiseGuiScriptingView.prototype.stopUserScript = function() {

	this.editorStopButton.attr('disabled', true);
	this.editorStartButton.attr('disabled', false);

	if (this.userScript && "stop" in this.userScript && typeof(this.userScript.stop) == "function") {
		this.userScript.stop();
	}

	var userScriptDomElem = document.getElementById('WiseGuiUserScriptDomElem-' + this.experimentId);
	if (this.userScriptDomElem && userScriptDomElem) {
		document.body.removeChild(userScriptDomElem);
	}
};

WiseGuiScriptingView.prototype.buildOutputView = function() {
	this.outputDiv = $('<div id="'+this.outputDivId+'"/>');
	this.outputView = this.outputDiv;
};

module.exports = WiseGuiScriptingView;
},{}],35:[function(require,module,exports){
var WiseGuiNodeSelectionDialog = require('./wisegui-nodeselection-dialog.js');
var WiseGuiOperationProgressView = require('./wisegui-operation-progress-view.js');

/**
 * #################################################################
 * WiseGuiSendView
 * #################################################################
 */

var WiseGuiSendView = function(reservation) {

	this.reservation         = reservation;
	this.experimentId        = reservation.experimentId;

	this.nodeSelectionButton = null;
	this.modeSelect          = null;
	this.messageInput        = null;
	this.sendButton          = null;
	this.lineFeedCheckbox    = null;

	this.buildView();
};

WiseGuiSendView.prototype.buildView = function() {

	this.view = $(
			'<div class="row">' +
			'	<button class="btn WiseGuiSendViewSelectNodesButton span2">Select Nodes</button>' +
			'	<div class="span2">' +
			'		<select class="WiseGuiSendViewModeSelect span2">' +
			'			<option value="binary">Binary</option>' +
			'			<option value="ascii">ASCII</option>' +
			'		</select>' +
			'	</div>' +
			'	<div class="span4">' +
			'		<input type="text" class="WiseGuiSendViewMessageInput span4"/>' +
			'	</div>' +
			'	<div class="span2">' +
			'		<button class="btn btn-primary WiseGuiSendViewSendButton span2">Send message</button><br/>' +
			'	</div>' +
			'</div>' +
			'<div class="row">' +
			'	<div class="offset4 span8">' +
			'		<div class="inputs-list">' +
			'			<label class="checkbox inline"><input class="WiseGuiSendViewLineFeedCheckbox" type="checkbox"> always append line feed</label>' +
			'		</div>' +
			'	</div>' +
			'</div>'
	);

	this.nodeSelectionButton = this.view.find('button.WiseGuiSendViewSelectNodesButton').first();
	this.modeSelect          = this.view.find('select.WiseGuiSendViewModeSelect').first();
	this.messageInput        = this.view.find('input.WiseGuiSendViewMessageInput').first();
	this.sendButton          = this.view.find('button.WiseGuiSendViewSendButton').first();
	this.lineFeedCheckbox    = this.view.find('input.WiseGuiSendViewLineFeedCheckbox').first()[0];

	var self = this;

	this.nodeSelectionDialog = new WiseGuiNodeSelectionDialog(
			this.experimentId,
			'Select Node URNs',
			'Please select the nodes to which you want to send a message.',
			null,
			'nodeselection.send.'
	);

	this.nodeSelectionButton.bind('click', self, function(e) {
		self.nodeSelectionDialog.show(function(){
			self.updateControls();
		});
	});

	this.sendButton.bind('click', self, function(e) { self.onMessageButtonClicked(); });

	this.messageInput.bind('keyup', self, function(e) {
		self.updateControls();
		// send on 'enter' (keycode 13)
		if (e.which == 13 && self.isMessageInputValid() && self.isMessageNodesSelectionValid()) {
			self.onMessageButtonClicked();
		}
	});
	
	this.messageInput.popover({
		placement : 'bottom',
		trigger   : 'manual',
		animation : true,
		content   : 'The message must consist of comma-separated bytes in base_10 (no prefix), base_2 (prefix 0b) or base_16 (prefix 0x).<br/>' +
					'<br/>' +
					'Example: <code>0x0A,0x1B,0b11001001,40,80</code>',
		title     : 'Message Format'
	});
	
	this.messageInput.focusin(function() {
		if (self.getMode() == 'binary') {
			self.messageInput.popover("show");
		}
	});
	
	this.messageInput.focusout(function() {
		if (self.getMode() == 'binary') {
			self.messageInput.popover("hide");
		}
	});

	this.updateControls();

	WiseGui.bindToReservationState(this.view.find('button'), this.experimentId);
	WiseGui.bindToReservationState(this.view.find('input'), this.experimentId);
	WiseGui.bindToReservationState(this.view.find('select'), this.experimentId);
};

WiseGuiSendView.prototype.send = function(targetNodeUrns, payloadBase64) {

	for (var i=0; i<targetNodeUrns.length; i++) {
		var message = {
			targetNodeUrn : targetNodeUrns[i],
			payloadBase64 : payloadBase64
		};
		this.socket.send(JSON.stringify(message));
	}
};

WiseGuiSendView.prototype.isMessageInputValid = function() {
	return this.parseMessagePayloadBase64() !== null;
};

WiseGuiSendView.prototype.isMessageNodesSelectionValid = function() {
	return this.getSelectedNodeUrns().length > 0;
};

WiseGuiSendView.prototype.getSelectedNodeUrns = function() {
	return this.nodeSelectionDialog.getSelection();
};

WiseGuiSendView.prototype.updateControls = function() {

	var nodes = this.nodeSelectionDialog.getSelection();
	this.nodeSelectionButton.html(nodes.length == 1 ? "1 node selected" : nodes.length + ' nodes selected');

	if (this.isMessageInputValid()) {
		this.messageInput.removeClass('error');
	} else {
		this.messageInput.addClass('error');
	}

	this.sendButton.attr('disabled', !(this.isMessageInputValid() && this.isMessageNodesSelectionValid()));
};

WiseGuiSendView.prototype.onMessageButtonClicked = function() {

	var self = this;
	this.sendButton.attr('disabled', true);
	var nodeUrns = this.getSelectedNodeUrns();

	wisebed.experiments.send(
			this.experimentId,
			nodeUrns,
			this.parseMessagePayloadBase64(),
			function(result) {
				var progressView = new WiseGuiOperationProgressView(
						nodeUrns, 1,
						"The message was sent successfully to all nodes."
				);
				progressView.update(result);
				self.sendButton.attr('disabled', false);
			},
			function(jqXHR, textStatus, errorThrown) {
				self.sendButton.attr('disabled', false);
				WiseGui.showAjaxError(jqXHR, textStatus, errorThrown);
			}
	);
};

WiseGuiSendView.prototype.parseMessagePayloadBase64 = function() {

	var messageBytes;
	var messageString = this.messageInput[0].value;

	if (messageString === undefined || '') {
		return null;
	}
	
	if (this.lineFeedCheckbox.checked) {
		messageString += '\n';
	}

	messageBytes = this.getMode() == 'binary' ?
			this.parseByteArrayFromString(messageString) :
			this.parseByteArrayFromAsciiString(messageString);

	return messageBytes === null ? null : base64_encode(messageBytes);
};

WiseGuiSendView.prototype.getMode = function() {
	return this.modeSelect[0].options[this.modeSelect[0].selectedIndex].value;
};

WiseGuiSendView.prototype.parseByteArrayFromAsciiString = function(messageString) {

	if (messageString === null || messageString === '') {
		return null;
	}

	var messageBytes = [];
	for(var i = 0; i < messageString.length; i++) {
		messageBytes[i] = messageString.charCodeAt(i);
	}

	return messageBytes;
};

WiseGuiSendView.prototype.parseByteArrayFromString = function(messageString) {

	var splitMessage = messageString.split(",");
	var messageBytes = [];

	for (var i=0; i < splitMessage.length; i++) {

		splitMessage[i] = splitMessage[i].replace(/ /g, '');

		var radix = 10;

		if (splitMessage[i].indexOf("0x") === 0) {

			radix = 16;
			splitMessage[i] = splitMessage[i].replace("0x","");

		} else if (splitMessage[i].indexOf("0b") === 0) {

			radix = 2;
			splitMessage[i] = splitMessage[i].replace("0b","");

			if (/^(0|1)*$/.exec(splitMessage[i]) === null) {
				return null;
			}

		}

		messageBytes[i] = parseInt(splitMessage[i], radix);

		if (isNaN(messageBytes[i])) {
			return null;
		}
	}

	if (messageBytes.length === 0) {
		return null;
	}

	return messageBytes;
};

module.exports = WiseGuiSendView;
},{"./wisegui-nodeselection-dialog.js":25,"./wisegui-operation-progress-view.js":28}],36:[function(require,module,exports){
/**
 * #################################################################
 * WiseGuiTable
 * #################################################################
 */

var WiseGuiTableElem = function (data) {
	this.data = data;
	this.row = null;
	this.isVisible = true;
	this.checkbox = null;
};

/**
 * Model: Object[]
 * headers: String[]
 * rowProducer: fun(obj) -> String[]
 * preFilterFun: fun(obj) -> true | false
 * preSelectFun: fun(obj) -> true | false
 * showCheckBoxes: true | false
 * showFilterBox: true | false
 */
var WiseGuiTable = function (model, headers, rowProducer, preFilterFun, preSelectFun, showCheckBoxes, showFilterBox, options) {

	this.model              = model;
	this.headers            = headers;
	this.rowProducer        = rowProducer;
	this.preFilterFun       = preFilterFun;
	this.preSelectFun       = preSelectFun;
	this.showCheckBoxes     = showCheckBoxes;
	this.options            = options;

	this.pagination         = this.options && this.options.pagination;
	this.paginationOffset   = this.options.paginationOffset !== undefined ? this.options.paginationOffset : 0;
	this.paginationAmount   = this.options.paginationAmount !== undefined ? this.options.paginationAmount : 10;

	this.sortOptions        = {
		headers : options.sortHeaders || (this.showCheckBoxes ? { 0 : {sorter : false}} : undefined),
		sortList : [[(this.options.sortColumn || 0) + (this.showCheckBoxes ? 1 : 0), this.options.sortAsc ? 0 : 1]]
	};

	this.html               = $("<div></div>");
	this.table              = null;
	this.filter             = null;
	this.data               = [];
	this.selectionListeners = [];
	this.filterListeners    = [];

	this.filter_input       = null;
	this.input_checkbox_th  = null;

	if(showFilterBox) {
		this.lastWorkingFilterExpr = null;
		this.filter_checkbox = null;
		this.generateFilter();
	}

	this.generateTable();

	if(this.preFilterFun) {
		this.setFilterFun(this.preFilterFun);
	}

	if(this.preSelectFun) {
		this.setSelectFun(this.preSelectFun);
	}

	return this;
};

WiseGuiTable.prototype.generateFilter = function () {
	var that = this;

	// Filter
	this.filter = $('<p style="margin-top:3px;"></p>');

	var img_help = $('<img class="WiseGuiNodeTable" style="float:right;cursor:pointer;margin-top:5px;">');
	img_help.attr("src", "img/famfamfam/help.png");

	var div_help = $('<div style="margin-right:95px;"></div>');
	var div_adv = $('<div style="float:right;margin-top:3px;margin-right:2px;">Advanced</div>');

	this.filter_checkbox = $('<input type="checkbox" style="float:right;margin-top:7px;margin-right:3px;">');
	this.filter.append(img_help, div_adv, this.filter_checkbox, div_help);

	var filter_input = $('<input type="text" placeholder="Filter displayed nodes...">');
	// Key up event if enter is pressed
	filter_input.keyup(function(e) {
		if ((e.keyCode || e.which) == 13) {
			var filter_fun = that.setFilterFun.bind(that);
			var val = filter_input.val();
			filter_fun(val);
		}
	});
	this.filter_input = filter_input;

	var helpTooltipIsVisible = false;
	img_help.click(function() {
		img_help.popover(helpTooltipIsVisible ? 'hide' : 'show');
		helpTooltipIsVisible = !helpTooltipIsVisible;
	});

	var helpText = '<h3>Normal mode</h3>';
	helpText += 'In normal mode, the filter is a full text search.';
	helpText += '<h3>Advanced mode</h3>';
	helpText += 'In advanced mode, the filter is using <a href="http://api.jquery.com/filter/" target="_blank">jQuery.filter()</a> on the given data structure.';

	if(this.model.length > 0) {
		helpText += '<br>The data structure looks as follows:';
		helpText += "<pre style=\"overflow:auto;height:50px;margin:0px;\">" + JSON.stringify(this.model[0], wiseMLNullFilter, '  ') + "</pre>";
	}

	helpText += '<h5>Some examples:</h5>';

	helpText += '<ul style="margin-bottom:0px;font-family: monospace;">';
	helpText += '<li>e.nodeType == "isense"';
	helpText += '<li>e.position.x == 25';
	helpText += '<li>e.id.indexOf("0x21") > 0';
	helpText += '<li>($(e.capability).filter(function (i) {return this.name.indexOf("temperature") > 0;}).length > 0)';
	helpText += '</ul>';

	img_help.popover({
		placement:'left',
		animation:true,
		trigger: 'manual',
		content: helpText,
		title: "Filter Help"
	});
	div_help.append(filter_input);
	this.html.append(this.filter);
};

WiseGuiTable.prototype.generateTable = function () {

	var that = this;

	// Prepare the WiseGuiTableElems
	$(this.model).each(
		function() {
			that.data.push(new WiseGuiTableElem(this));
		}
	);

	this.table = $('<table class="table table-bordered"></table>');

	/*
	 * Generate table header
	 */
	var thead = $('<thead></thead>');
	var tr_thead = $('<tr></tr>');
	thead.append(tr_thead);

	// Reusable stuff
	var th = $('<th class="header"></th>');
	var input_checkbox = $('<input type="checkbox"/>');

	// Append the checkbox to the header
	if(this.showCheckBoxes) {
		var th_checkbox = th.clone();
		var input_checkbox_th = input_checkbox.clone();

		input_checkbox_th.click(function() {
			var checked = $(this).is(':checked');
			if(that.table !== null) {
				// .find("input")
				var inputs = that.table.find('tr:visible').find('input:checkbox');
				inputs.each(function(index, input) {
					$(this).attr('checked', checked);
					if(index>0) // first checkbox does not belong to a specific node and has no urn
					that.callSelectionListeners(input.attributes.urn.nodeValue,!checked);
				});
			}
		});
		th_checkbox.append(input_checkbox_th);
		this.input_checkbox_th = input_checkbox_th;
		tr_thead.append(th_checkbox);
	}

	$.each(this.headers,
		function(key, value) {
			var th_local = th.clone();
			th_local.append(value);
			tr_thead.append(th_local);
		}
	);

	this.table.append(thead);
	this.table.append($('<tbody></tbody>'));
	this.html.append(this.table);

	if (this.pagination) {

		this.paginationView = $(
			'<div class="pagination pagination-centered">' +
			'	<ul>' +
			'	</ul>' +
			'</div>'
		);
		var pages = Math.ceil(this.data.length / this.paginationAmount);

		var self = this;

		for (var page=0; page<pages; page++) {

			var li = $('<li data-page="'+page+'"></li>');
			var a = $('<a href="#">'+(page+1)+'</a>');

			this.paginationView.find('ul').append(li.append(a));
			
			a.data('page', page);
			/*jshint loopfunc:true */
			a.click(function(e) {
				e.preventDefault();
				self.gotoPage($(this).data('page'));
			});
		}

		this.html.append(this.paginationView);
		this.gotoPage(0);

	} else {
		this.renderTableContents();
	}

	if (this.data.length > 0) {
		this.table.tablesorter(this.sortOptions);
	}
};

WiseGuiTable.prototype.gotoPage = function(page) {
	this.paginationView.find('li').removeClass('active');
	this.paginationView.find('li[data-page="'+page+'"]').addClass('active');
	this.paginationOffset = page * (this.paginationAmount);
	this.renderTableContents();
};

WiseGuiTable.prototype.renderTableContents = function() {
	var self = this;
	var tbody = this.table.find('tbody');
	tbody.empty();

	if (this.rowProducer !== null) {

		var offset = this.pagination ? this.paginationOffset : 0;
		var amount = this.pagination ? this.paginationAmount : this.data.length;
		var actualAmount = (offset + amount > this.data.length) ? this.data.length : (offset + amount);

		for (var i = offset; i < actualAmount; i++) {

			var data = this.data[i].data;

			var row = null;
			if(this.rowProducer !== null) {
				row = this.rowProducer.bind(data)(data);
			}

			var tr = $("<tr></tr>");

			if(this.showCheckBoxes) {
				var checkbox = $('<input type="checkbox"/>');
				checkbox.attr("name", i);
				checkbox.attr("urn", data.id);
				/*jshint loopfunc:true */
				checkbox.click(function() {
						var checked = $(this).is(':checked');
						self.callSelectionListeners(this.attributes.urn.nodeValue, !checked);
					});
				data.checkbox = checkbox;
				var td_checkbox = $('<td></td>');
				td_checkbox.append(checkbox);
				tr.append(td_checkbox);
			}

			for(var j = 0; j<row.length; j++) {
				var td = $('<td></td>');
				td.append(row[j]);
				tr.append(td);
			}
			this.data[i].row = tr;
			tbody.append(tr);
		}
	}

	if (this.data.length === 0) {
		var noDataMessage = this.options && this.options.noDataMessage ? this.options.noDataMessage : 'No data available.';
		tbody.append($('<tr><td colspan="'+this.headers.length+'">'+noDataMessage+'<td></tr>'));
	}
};

WiseGuiTable.prototype.addSelectionListener = function (listener) {
	this.selectionListeners.push(listener);
};

WiseGuiTable.prototype.addFilterListener = function ( listener) {
	this.filterListeners.push(listener);
};

WiseGuiTable.prototype.callSelectionListeners = function(urn, deselected){
	var that = this;
	for (var i =0; i<that.selectionListeners.length;i++){
		that.selectionListeners[i](urn, deselected);
	}
};

WiseGuiTable.prototype.callFilterListeners = function(urns){
	var that = this;
	for (var i =0; i<that.filterListeners.length;i++){
		that.filterListeners[i](urns);
	}
};

WiseGuiTable.prototype.getSelectedRows = function () {

	var that = this;

	var selected = [];
	if(this.data !== null && this.table !== null) {
		this.table.find("input:checked").each(function() {
			var name = $(this).attr('name');
			// Ignore the checkbox from the header, which doesn't have any name
			if(typeof(name) != "undefined") {
				var index = parseInt(name);
				selected.push(that.data[index].data);
			}
		});
	}
	return selected;
};

WiseGuiTable.prototype.setSelectedRows = function (selectionCallback) {

	var that = this;

	if(this.data !== null && this.table !== null) {
		this.table.find("input").each(function() {
			var checkbox = $(this);
			var name = $(this).attr('name');
			// Ignore the checkbox from the header, which doesn't have any name
			if(typeof(name) != "undefined") {
				var index = parseInt(name);
				checkbox.prop('checked', selectionCallback(that.data[index].data));
			}
		});
	}
};

WiseGuiTable.prototype.setFilterFun = function (fn) {

	this.preFilterFun = fn;

	for ( var i = 0; i < this.data.length; i++) {
		var d = this.data[i];
		d.isVisible = true; // Reset

		if(fn !== null && typeof(fn) == "function") {
			d.isVisible = d.isVisible && fn.bind(d.data)(d.data);
		} else if(fn !== null && typeof(fn) == "string" && fn.length > 0 && this.filter_checkbox.is(':checked')) {
			// Filter
			var errorOccured = false;

			/*jshint loopfunc:true */
			var fil = function(e) {
				ret = true;
				try {
					/*jshint evil:true */
					ret = eval(fn);
				} catch (ex) {
					errorOccured = true;
					ret = null;
				}

				if(typeof(ret) != "boolean") {
					if(that.lastWorkingFilterExpr !== null) {
						/*jshint evil:true */
						ret = eval(that.lastWorkingFilterExpr);
					} else {
						return true;
					}
				} else {
					return ret;
				}
			};

			d.isVisible = d.isVisible && fil(d.data);

			if(errorOccured) {
				WiseGui.showErrorAlert("Filter expression invalid.");
				return;
			} else {
				this.lastWorkingFilterExpr = fn;
			}
		}

		// Simple filter
		if(fn !== null && typeof(fn) == "string" && fn.length > 0 && !this.filter_checkbox.is(':checked')) {
			if(this.rowProducer !== null) {
				var row = this.rowProducer(d.data);
				if(implode(" ", row).toLowerCase().indexOf(fn.toLowerCase()) < 0) {
					d.isVisible = false;
				}
			}
		}

		if(d.isVisible) {
			d.row.show();
		} else {
			d.row.hide();
		}
	}

	if(this.showCheckBoxes) {
		this.input_checkbox_th.attr('checked', false);
	}
	var urns = [];
	for ( var j = 0; j < this.data.length; j++) {
		var e = this.data[j];
		if(e.isVisible === true){
			urns.push(e.data.id);}
		}
	this.callFilterListeners(urns);
};

WiseGuiTable.prototype.setSelectFun = function (fn) {

	this.preSelectFun = fn;

	for ( var i = 0; i < this.data.length; i++) {
		var data = this.data[i].data;
		var bool = false;
		if(fn !== null) {
			bool = fn.bind(data)(data);
		}
		var checkbox = this.data[i].row.find('input:checkbox');
		checkbox.attr('checked', bool);
	}
};

WiseGuiTable.prototype.getFilterFun = function () {
	return this.preFilterFun;
};

WiseGuiTable.prototype.getSelectFun = function () {
	return this.preSelectFun;
};

module.exports = WiseGuiTable;

},{}],37:[function(require,module,exports){
(function (global){
global._                       = require('underscore');
var toastr                     = require('toastr');
var wjs                        = require('wisebed.js');
var WiseGuiNodeTable           = require('./wisegui-node-table.js');
var WiseGuiReservationDialog   = require('./wisegui-reservation-dialog.js');
var WiseGuiReservationObserver = require('./wisegui-reservation-observer.js');
var WiseGuiReservationView     = require('./wisegui-reservation-view.js');
var WiseGuiLoginObserver       = require('./wisegui-login-observer.js');
var WiseGuiTable               = require('./wisegui-table.js');
var WiseGuiNavigationView      = require('./wisegui-navigation-view.js');
var WiseGuiGoogleMapsView      = require('./wisegui-google-maps-view.js');
var WiseGuiModalDialog         = require('./wisegui-modal-dialog.js');
var WiseGuiEvents              = require('./wisegui-events.js');

$(window).bind(WiseGuiEvents.EVENT_NOTIFICATION, function (event, alert) {
	
	var div = $('<div/>');

	var formatMessage = function(message) {
		// http://stackoverflow.com/questions/783818/how-do-i-create-a-custom-error-in-javascript
		if (message instanceof Error && message.stack) {
			return alert.message.stack;
		}
		return message;
	};

	if (alert.message instanceof Array) {
		alert.message.map(formatMessage).forEach(div.append);
	} else {
		div.append(formatMessage(alert.message));
	}
	var autoHide = (alert.severity == 'success');
	
	if (alert.severity == 'warning') {
		toastr.options = {
			"closeButton": true,
			"debug": false,
			"positionClass": "toast-bottom-right",
			"onclick": null,
			"showDuration": 300,
			"hideDuration": 10,
			"timeOut": 10000,
			"extendedTimeOut": 10000,
			"showMethod": "fadeIn",
			"hideMethod": "fadeOut"
		};
		toastr.warning(div);
	} else if (alert.severity == 'error') {
		toastr.options = {
			"closeButton": true,
			"debug": false,
			"positionClass": "toast-bottom-right",
			"onclick": null,
			"showDuration": 300,
			"hideDuration": 10,
			"timeOut": 0,
			"extendedTimeOut": 0,
			"showMethod": "fadeIn",
			"hideMethod": "fadeOut"
		};
		toastr.error(div);
	} else if (alert.severity == 'success') {
		toastr.options = {
			"closeButton": true,
			"debug": false,
			"positionClass": "toast-bottom-right",
			"onclick": null,
			"showDuration": 300,
			"hideDuration": 10,
			"timeOut": 5000,
			"extendedTimeOut": 5000,
			"showMethod": "fadeIn",
			"hideMethod": "fadeOut"
		};
		toastr.success(div);
	} else {
		toastr.options = {
			"closeButton": true,
			"debug": false,
			"positionClass": "toast-bottom-right",
			"onclick": null,
			"showDuration": 300,
			"hideDuration": 100,
			"timeOut": 120000,
			"extendedTimeOut": 120000,
			"showMethod": "fadeIn",
			"hideMethod": "fadeOut"
		};
		toastr.info(div);
	}
});

global.WiseGui = {

	showAlert : function(message, severity) {
		$(window).trigger(WiseGuiEvents.EVENT_NOTIFICATION,
				{
					severity : severity,
					message  : message
				}
		);
	},
	showBlockAlert : function(message, actions, severity) {
		$(window).trigger(WiseGuiEvents.EVENT_NOTIFICATION,
				{
					severity : severity,
					message  : message,
					actions  : actions
				}
		);
	},
	showWarningAlert : function(message) {
		WiseGui.showAlert(message, 'warning');
	},
	showErrorAlert : function(message) {
		WiseGui.showAlert(message, 'error');
	},
	showSuccessAlert : function(message) {
		WiseGui.showAlert(message, 'success');
	},
	showInfoAlert : function(message) {
		WiseGui.showAlert(message, 'info');
	},
	showWarningBlockAlert : function(message, actions) {
		WiseGui.showBlockAlert(message, actions, 'warning');
	},
	showErrorBlockAlert : function(message, actions) {
		WiseGui.showBlockAlert(message, actions, 'error');
	},
	showSuccessBlockAlert : function(message, actions) {
		WiseGui.showBlockAlert(message, actions, 'success');
	},
	showInfoBlockAlert : function(message, actions) {
		WiseGui.showBlockAlert(message, actions, 'info');
	},
	showAjaxError : function(jqXHR, textStatus, errorThrown) {
		console.log(jqXHR);
		var message = '= Error while loading data!\n';
		message    += '== jqXHR';
		message    += (jqXHR.readyState   !== undefined ? ('readyState = ' + jqXHR.readyState + '\n') : '');
		message    += (jqXHR.status       !== undefined ? ('status = ' + jqXHR.status + '\n') : '');
		message    += (jqXHR.responseText !== undefined ? ('responseText = ' + jqXHR.responseText + '\n') : '');
		/*
		var message = $(
			'<h2>Error while loading data!</h2>' +
			'<h3>jqXHR</h3>' +
			(jqXHR.readyState   !== undefined ? ('readyState = ' + jqXHR.readyState + '<br/>') : '') +
			(jqXHR.status       !== undefined ? ('status = ' + jqXHR.status + '<br/>') : '') +
			(jqXHR.responseText !== undefined ? ('responseText = <pre>' + jqXHR.responseText + '</pre><br/>') : '') +
			'<h3>textStatus</h3>' +
			'<pre>' + textStatus + '</pre>' +
			'<h3>errorThrown</h3>' +
			'<pre>' + errorThrown + '</pre>'
		);*/
		WiseGui.showErrorBlockAlert(message);
	},
	bindToReservationState : function(elems, experimentId) {

		$.each(elems, function(index,e) {

			var elem = $(e);
			var originalDisabled = elem.attr('disabled') == 'disabled';

			elem.attr('disabled', 'disabled');

			$(window).bind(WiseGuiEvents.EVENT_RESERVATION_STARTED, function(e, reservation) {
				if (experimentId == reservation.experimentId) {
					
					if (originalDisabled) {
						elem.attr('disabled', 'disabled');
					} else {
						elem.removeAttr('disabled');
					}
				}
			});

			$(window).bind(WiseGuiEvents.EVENT_RESERVATION_ENDED, function(e, reservation) {
				if (experimentId == reservation.experimentId) {
					elem.attr('disabled', 'disabled');
				}
			});
		});

		return elems;
	}
};

global.checkLoggedIn = function(callback) {
	wisebed.isLoggedIn(callback, WiseGui.showAjaxError);
};

global.doLogin = function(loginData) {

	var self = this;

	var callbackError = function(jqXHR, textStatus, errorThrown) {
		$(window).trigger(WiseGuiEvents.EVENT_LOGIN_ERROR, {
			jqXHR       : jqXHR,
			textStatus  : textStatus,
			errorThrown : errorThrown
		});
	};

	var callbackDone = function() {
		$(window).trigger(WiseGuiEvents.EVENT_LOGGED_IN, { loginData : self.loginData });
		$(window).trigger('hashchange');
	};

	wisebed.login(loginData, callbackDone, callbackError);
};

global.doLogout = function() {

	var callbackOK = function() {
		$(window).trigger(WiseGuiEvents.EVENT_LOGGED_OUT);
		$('#WiseGuiLoginDialog').remove();
		navigateTo(undefined, 'WiseGuiTestbedDetailsMapView');
	};

	var callbackError = function(jqXHR, textStatus, errorThrown) {
		WiseGui.showErrorAlert("Logout failed.");
	};

	wisebed.logout(callbackOK, callbackError);
	
};

global.wiseMLNullFilter = function(key,value) {
	if (value === null || value === undefined || (value instanceof Array && value.length === 0)) {
		return undefined;
	} else {
		return value;
	}
};

global.loadTestbedDetailsContainer = function(navigationData, parentDiv) {

	parentDiv.append($('<h2 class="WiseGuiTestbedTitle">'+testbedDescription.name+'</h2>'));

	var tabsId                          = 'WiseGuiTestbedDetailsTabs';
	var mapTabDivId                     = 'WiseGuiTestbedDetailsMapView';
	var nodesTabDivId                   = 'WiseGuiTestbedDetailsNodes';
	var reservationsTabDivId            = 'WiseGuiTestbedDetailsReservations';
	var myReservationsTabDivId          = 'WiseGuiTestbedDetailsMyReservations';
	var federatableReservationsTabDivId = 'WiseGuiTestbedDetailsFederatableReservations';
	var wiseMLXMLTabDivId               = 'WiseGuiTestbedDetailsWiseMLXML';
	var wiseMLJSONTabDivId              = 'WiseGuiTestbedDetailsWiseMLJSON';

	var tabs = $(
			'<ul class="nav nav-tabs" id="'+tabsId+'">' +
			'	<li class="active"    ><a href="#'+mapTabDivId+'">Map</a></li>' +
			'	<li                   ><a href="#'+nodesTabDivId+'">Nodes</a></li>' +
			'	<li                   ><a href="#'+reservationsTabDivId+'">All Reservations</a></li>' +
			'	<li                   ><a href="#'+myReservationsTabDivId+'">My Reservations</a></li>' +
			'	<li                   ><a href="#'+federatableReservationsTabDivId+'">Federatable Reservations</a></li>' +
			'	<li class="pull-right"><a href="#'+wiseMLXMLTabDivId+'">WiseML (XML)</a></li>' +
			'	<li class="pull-right"><a href="#'+wiseMLJSONTabDivId+'">WiseML (JSON)</a></li>' +
			'</ul>' +
			'<div class="tab-content">' +
			'	<div class="tab-pane active" id="'+mapTabDivId+'"/>' +
			'	<div class="tab-pane       " id="'+nodesTabDivId+'"/>' +
			'	<div class="tab-pane       " id="'+reservationsTabDivId+'"/>' +
			'	<div class="tab-pane       " id="'+myReservationsTabDivId+'"/>' +
			'	<div class="tab-pane       " id="'+federatableReservationsTabDivId+'"/>' +
			' <div class="tab-pane       " id="'+wiseMLXMLTabDivId+'"/>' +
			'	<div class="tab-pane       " id="'+wiseMLJSONTabDivId+'"/>' +
			'</div>'
	);

	var mapTabContentDiv                     = tabs.find('#'+mapTabDivId).first();
	var nodesTabContentDiv                   = tabs.find('#'+nodesTabDivId).first();
	var reservationsTabContentDiv            = tabs.find('#'+reservationsTabDivId).first();
	var myReservationsTabContentDiv          = tabs.find('#'+myReservationsTabDivId).first();
	var federatableReservationsTabContentDiv = tabs.find('#'+federatableReservationsTabDivId).first();
	var wiseMLXMLTabContentDiv               = tabs.find('#'+wiseMLXMLTabDivId).first();
	var wiseMLJSONTabContentDiv              = tabs.find('#'+wiseMLJSONTabDivId).first();

	var myReservationsTab                    = tabs.find('a[href="#'+myReservationsTabDivId+'"]').first();
	var federatableReservationsTab           = tabs.find('a[href="#'+federatableReservationsTabDivId+'"]').first();

	parentDiv.append(tabs);
	myReservationsTab.hide();
	federatableReservationsTab.hide();

	var reloadMapsTab = function() {
		mapTabContentDiv.empty();
		buildMapsView(mapTabContentDiv);
	};

	var reloadNodesTab = function() {
		nodesTabContentDiv.empty();
		wisebed.getWiseMLAsJSON(
				null,
				function(wiseML) {
					new WiseGuiNodeTable(wiseML, nodesTabContentDiv, false, true);
				},
				WiseGui.showAjaxError
		);
	};

	var reloadReservationsTab = function() {
		reservationsTabContentDiv.empty();
		buildReservationTable(reservationsTabContentDiv);
	};

	var reloadMyReservationsTab = function() {
		if (isLoggedIn) {
			myReservationsTabContentDiv.empty();
			buildMyReservationTable(myReservationsTabContentDiv);
		}
	};

	var reloadFederatableReservationsTab = function() {
		if (testbedDescription.isFederator && isLoggedIn) {
			federatableReservationsTabContentDiv.empty();
			buildFederatableReservationTable(federatableReservationsTabContentDiv);
		}
	};

	var reloadWiseMLXMLTab = function() {
		wiseMLXMLTabContentDiv.empty();
		wisebed.getWiseMLAsXML(
				null,
				function(wiseML) {
					wiseMLXMLTabContentDiv.append($('<pre class="WiseGuiTestbedDetailsWiseMLXML">' + wiseML.replace(/</g,"&lt;") + '</pre>'));
					wiseMLXMLTabContentDiv.append($('<a href="' + wisebedBaseUrl + '/experiments/network.xml" target="_blank" class="btn btn-primary pull-right">Download</a>'));
				},
				WiseGui.showAjaxError
		);
	};

	var reloadWiseMLJSONTab = function() {
		wiseMLJSONTabContentDiv.empty();
		wisebed.getWiseMLAsJSON(
				null,
				function(wiseML) {
					wiseMLJSONTabContentDiv.append($('<pre class="WiseGuiTestbedDetailsWiseMLJSON">' + JSON.stringify(wiseML, wiseMLNullFilter, '  ') + '</pre>'));
					wiseMLJSONTabContentDiv.append($('<a href="' + wisebedBaseUrl + '/experiments/network.json" target="_blank" class="btn btn-primary pull-right">Download</a>'));
				},
				WiseGui.showAjaxError
		);
	};

	var reloadFunctions = {};
	reloadFunctions[mapTabDivId]                     = reloadMapsTab;
	reloadFunctions[nodesTabDivId]                   = reloadNodesTab;
	reloadFunctions[reservationsTabDivId]            = reloadReservationsTab;
	reloadFunctions[myReservationsTabDivId]          = reloadMyReservationsTab;
	reloadFunctions[federatableReservationsTabDivId] = reloadFederatableReservationsTab;
	reloadFunctions[wiseMLXMLTabDivId]               = reloadWiseMLXMLTab;
	reloadFunctions[wiseMLJSONTabDivId]              = reloadWiseMLJSONTab;

	var self = this;
	$(window).bind(WiseGuiEvents.EVENT_NAVIGATION, function(e, navigationData) {
		if (navigationData.nav == 'overview' && navigationData.tab) {
			tabs.find('a[href="#'+navigationData.tab+'"]').tab('show');
			var reloadFunction = reloadFunctions[navigationData.tab];
			if (reloadFunction) {
				reloadFunction();
			}
		} else if (navigationData.nav == 'overview' && navigationData.tab === '') {
			reloadMapsTab();
		}
	});
	
	$(window).bind(WiseGuiEvents.EVENT_RESERVATIONS_CHANGED, function() {
		buildReservationTable(reservationsTabContentDiv);
		if (isLoggedIn) {
			buildMyReservationTable(myReservationsTabContentDiv);
		}
	});
	
	$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function() {
		myReservationsTab.show();
		buildMyReservationTable(myReservationsTabContentDiv);
	});
	
	$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function() {
		myReservationsTab.hide();
		myReservationsTabContentDiv.empty();
	});
	
	if (testbedDescription.isFederator) {
		
		$(window).bind(WiseGuiEvents.EVENT_LOGGED_IN, function() {
			federatableReservationsTab.show();
			buildFederatableReservationTable(federatableReservationsTabContentDiv);
		});

		$(window).bind(WiseGuiEvents.EVENT_LOGGED_OUT, function() {
			federatableReservationsTab.hide();
		});

		$(window).bind(WiseGuiEvents.EVENT_RESERVATIONS_CHANGED, function() {
			
			if (isLoggedIn) {
				buildFederatableReservationTable(federatableReservationsTabContentDiv);
			}

			// detect if any of the reservations tabs is visible and reload it if so
			var nav = getNavigationData();

			if (nav.tab == 'WiseGuiTestbedDetailsReservations') {
				reloadReservationsTab();
			}

			if (nav.tab == 'WiseGuiTestbedDetailsMyReservations') {
				reloadMyReservationsTab();
			}
		});
	}

	tabs.find('a').click(function (e) {
	    e.preventDefault();
	    var navigationData = getNavigationData();
	    navigationData.tab = e.target.hash.substring(1);
	    navigateToNavigationData(navigationData);
	});
};

global.buildMapsView = function(parent) {

	if (testbedDescription.isFederator) {
		
		wisebed.getWiseMLAsJSON(null, function(wiseML) {
		
			var wiseMLs = {
				'federated' : wiseML
			};

			// create tab pane
			var pills = $(
					'<div class="tabbable">' +
					'	<ul class="nav nav-pills"></ul>' +
					'	<div class="tab-content"></div>' +
					'</div>'
			);
			
			var headers = pills.find('ul.nav-pills');
			var contents = pills.find('div.tab-content');
			
			var federatedHeader = $('<li><a href="#WiseGuiMapsView-federated">Federated</a></li>');
			var federatedContent = $('<div class="tab-pane" id="WiseGuiMapsView-federated">Federated</div>');

			headers.append(federatedHeader);
			contents.append(federatedContent);

			// add a tab for each testbed URN prefix
			testbedDescription.urnPrefixes.forEach(function(nodeUrnPrefix, index) {

				wiseMLs[nodeUrnPrefix] = JSON.parse(JSON.stringify(wiseML));
				wiseMLs[nodeUrnPrefix].setup.node = wiseMLs[nodeUrnPrefix].setup.node.filter(function(node) {
					return node.id.indexOf(nodeUrnPrefix) === 0;
				});
				
				var header = $('<li><a href="#WiseGuiMapsView-'+index+'">'+nodeUrnPrefix+'</a></li>');
				var content = $('<div class="tab-pane" id="WiseGuiMapsView-'+index+'">'+nodeUrnPrefix+'</div>');

				$(header.find('a').first()).data('nodeUrnPrefix', nodeUrnPrefix);
				
				headers.append(header);
				contents.append(content);
			});

			pills.find('a').click(function(e) {
				
				e.preventDefault();
				$(this).tab('show');

				var nodeUrnPrefix = $(this).data('nodeUrnPrefix');
				var tab = $($(this).attr('href'));

				tab.empty();

				// init map
				var mapRow = $('<div class="row"><div class="span12"></div></div>');
				tab.append(mapRow);
				var wiseML = nodeUrnPrefix === undefined ? wiseMLs.federated : wiseMLs[nodeUrnPrefix];
				new WiseGuiGoogleMapsView(wiseML, mapRow.find('div').first());
			});

			parent.append(pills);
			pills.find('a').first().click();

		});

	} else {

		wisebed.getWiseMLAsJSON(
			null,
			function(wiseML) {
				// init description over map
				if (wiseML.setup && wiseML.setup.description) {
					var mapDescription = wiseML.setup.description;
					var mapDescriptionRow = $('<div class="row"><div class="span12">' + mapDescription + '</div></div>');
					parent.append(mapDescriptionRow);
				}
				// init map
				var mapRow = $('<div class="row"><div class="span12"></div></div>');
				parent.append(mapRow);
				new WiseGuiGoogleMapsView(wiseML, mapRow.find('div').first());
			},
			WiseGui.showAjaxError
		);
	}
};

global.buildFederatableReservationTable = function(parent) {
	
	var pills = $(
			'<div class="tabbable">' +
			'	<ul class="nav nav-pills">' +
			'		<li class="active"><a href="#WiseGuiFederatableReservationsCurrentFuture">Current and Upcoming</a></li>' +
			'	</ul>' +
			'	<div class="tab-content">' +
			'		<div class="tab-pane active" id="WiseGuiFederatableReservationsCurrentFuture"></div>' +
			'	</div>' +
			'</div>'
	);

	pills.find('a').click(function(e) {
		e.preventDefault();
		$(this).tab('show');
	});

	var currentAndFutureDiv = pills.find('#WiseGuiFederatableReservationsCurrentFuture');
	
	parent.empty();
	parent.append(pills);

	wisebed.reservations.getFederatable(
		moment(),
		null,
		function(federatableReservations) {
			buildPersonalReservationsTable(currentAndFutureDiv, federatableReservations);
		},
		WiseGui.showAjaxError
	);
};

global.buildMyReservationTable = function(parent) {

	var pills = $(
			'<div class="tabbable">' +
			'	<ul class="nav nav-pills">' +
			'		<li class="active"><a href="#WiseGuiMyReservationsCurrentFuture">Current and Upcoming</a></li>' +
			'		<li               ><a href="#WiseGuiMyReservationsPast">Past</a></li>' +
			'	</ul>' +
			'	<div class="tab-content">' +
			'		<div class="tab-pane active" id="WiseGuiMyReservationsCurrentFuture"></div>' +
			'		<div class="tab-pane"        id="WiseGuiMyReservationsPast"></div>' +
			'	</div>' +
			'</div>'
	);

	var currentAndFutureDiv = pills.find('#WiseGuiMyReservationsCurrentFuture');
	var pastDiv = pills.find('#WiseGuiMyReservationsPast');

	var loadPersonalReservations = function(past, showCancelled) {
		wisebed.reservations.getPersonal(
			!past ? moment() : null,
			 past ? moment() : null,
			function(reservations) {
				buildPersonalReservationsTable(!past ? currentAndFutureDiv : pastDiv, reservations, past);
			},
			WiseGui.showAjaxError,
			false,
			showCancelled
		);
	};

	pills.find('a[href="#WiseGuiMyReservationsCurrentFuture"]').click(function(e) {
		e.preventDefault();
		loadPersonalReservations(false, false);
		$(this).tab('show');
	});

	pills.find('a[href="#WiseGuiMyReservationsPast"]').click(function(e) {
		e.preventDefault();
		loadPersonalReservations(true, true);
		$(this).tab('show');
	});

	loadPersonalReservations(false, false);

	parent.empty();
	parent.append(pills);
};

global.buildPersonalReservationsTable = function(parent, reservations, past) {

	var nop = function(event){ event.preventDefault(); };

	var headers = ['From', 'Until', 'Testbed Prefix(es)', 'Nodes', 'Description', '', '', ''];
	var model = reservations;
	var rowProducer = function(reservation) {
		
		var rand = Math.floor(Math.random() * 100000);
		var rowData = [];
		
		rowData.push($('<a href="#" rel="tooltip" title="'+reservation.from.toISOString()+'">' + reservation.from.format("YYYY-MM-DD HH:mm:ss") + '</a>').tooltip('show').click(nop));
		rowData.push($('<a href="#" rel="tooltip" title="'+reservation.to.toISOString()+'">' + reservation.to.format("YYYY-MM-DD HH:mm:ss") + '</a>').tooltip('show').click(nop));
		rowData.push(reservation.nodeUrnPrefixes.join("<br/>"));
		rowData.push($(
				'<div>' +
				'	<a href="javascript:;" data-target="#wisegui-personal-reservation-nodes-'+rand+'" data-toggle="collapse">'+reservation.nodeUrns.length+' nodes</a>' +
				'	<div class="collapse" id="wisegui-personal-reservation-nodes-'+rand+'">'+reservation.nodeUrns.join("<br/>")+'</div>' +
				'</div>'
		));
		rowData.push(reservation.description);
		var openButton = $('<a class="btn btn-primary">Open</a>').bind('click', reservation, function(e) {
				e.preventDefault();
				if (openButton.attr('disabled') != 'disabled') {
					navigateTo(e.data.experimentId);
				}
		});
		if (reservation.finalized) {
			openButton.attr('disabled', 'disabled');
		}
		rowData.push(openButton);
		rowData.push($('<button class="btn" title="Download Log"><i class="icon-download"></i> Log</button>').bind('click', reservation, function(e) {
				var url = wisebedBaseUrl + '/events/' + e.data.experimentId + '.json';
				window.open(url, '_blank');
		}));
        
    	var cancelButton;
        var cancelledLabel;
    	var finalizedLabel;

    	if (reservation.cancelled) {
    		cancelledLabel = $('<span class="label label-important">Cancelled</span><br/><span class="label label-important">' + reservation.cancelled.format("YYYY-MM-DD HH:mm:ss") + '</span>');
    		cancelledLabel.popover({
    			placement : 'top',
				title   : 'Cancelled Reservation',
				content : '<p>This reservation has been <b>cancelled</b>.</p><p>A reservation ends '+
				 'either because the end of the reservaiton time span has been reached or because it has '+
				 'been cancelled. Due to network delays or temporary network disconnections in the testbed '+
				 'backend node outputs might still arrive delayed. After a couple of minutes after the '+
				 'last delayed node output was received the reservation is finalized, i.e. it is '+
				 'guaranteed that there will not be any more outputs attached to the reservations log.</p>'
			});
    	}

    	if (reservation.finalized) {
    		finalizedLabel = $('<span class="label label-info">Finalized</span>');
			finalizedLabel.popover({
				placement : 'top',
				title   : 'Finalized Reservation',
				content : '<p>This reservation has been <b>finalized</b>.</p><p>A reservation ends '+
				 'either because the end of the reservaiton time span has been reached or because it has '+
				 'been cancelled. Due to network delays or temporary network disconnections in the testbed '+
				 'backend node outputs might still arrive delayed. After a couple of minutes after the '+
				 'last delayed node output was received the reservation is finalized, i.e. it is '+
				 'guaranteed that there will not be any more outputs attached to the reservations log.</p>'
			});
		}

		if (!reservation.cancelled && !reservation.finalized && !past) {
			cancelButton = $('<a class="btn btn-danger">Cancel</a>').bind('click', reservation, function(e) {
	                e.preventDefault();
	                new WiseGuiModalDialog(undefined, function() {
	                	wisebed.reservations.cancel(
							e.data.experimentId,
							function() { 
								cancelButton.popover('hide');
								$(window).trigger('hashchange');
							},
							WiseGui.showAjaxError
						);
	                }).show();
	        });
			cancelButton.popover({
				placement : 'top',
				title   : 'Cancelling Reservations',
				content : 'Cancelling a reservation will free the resources bound to the reservation so that ' +
					'e.g. other users can reserve the nodes. If a reservation gets cancelled while it is ' +
					'active all reservation events (such as node outputs) until the cancellation will still ' +
					'be persisted in the reservation log and available for download.'
			});
		}

		if (reservation.cancelled && reservation.finalized) {
			rowData.push($('<span/>').append(cancelledLabel).append(' ').append(finalizedLabel));
		} else if (reservation.cancelled) {
			rowData.push(cancelledLabel);
		} else if (reservation.finalized) {
			rowData.push(finalizedLabel);
		} else if (!past) {
			rowData.push(cancelButton);
		} else {
			rowData.push('&nbsp;');
		}

		return rowData;
	};
	var preFilterFun = null;
	var preSelectFun = null;
	var showCheckBoxes = false;
	var showFilterBox = false;
	var options = {
		noDataMessage    : 'No reservations found.',
		pagination       : true,
		paginationAmount : 10
	};

	var table = new WiseGuiTable(model, headers, rowProducer, preFilterFun, preSelectFun, showCheckBoxes, showFilterBox, options);

	parent.empty();
	parent.append(table.html);
};

global.buildReservationTableInternal = function(parent, reservations) {

	var nop = function(event){ event.preventDefault(); };

	var headers = ['From', 'Until', 'Testbed Prefix(es)', 'Nodes', ' '];
	var model = reservations;
	var rowProducer = function(reservation) {
		
		var rand = Math.floor(Math.random() * 100000);
		var rowData = [];
		
		rowData.push($('<a href="#" rel="tooltip" title="'+reservation.from.toISOString()+'">' + reservation.from.format("YYYY-MM-DD HH:mm:ss") + '</a>').tooltip('show').click(nop));
		rowData.push($('<a href="#" rel="tooltip" title="'+reservation.to.toISOString()+'">' + reservation.to.format("YYYY-MM-DD HH:mm:ss") + '</a>').tooltip('show').click(nop));
		rowData.push(reservation.nodeUrnPrefixes.join("<br/>"));
		rowData.push($(
				'<div>' +
				'	<a href="javascript:;" data-target="#wisegui-personal-reservation-nodes-'+rand+'" data-toggle="collapse">'+reservation.nodeUrns.length+' nodes</a>' +
				'	<div class="collapse" id="wisegui-personal-reservation-nodes-'+rand+'">'+reservation.nodeUrns.join("<br/>")+'</div>' +
				'</div>'
		));

		var cancelledLabel;
		
		if (reservation.cancelled) {
			cancelledLabel = $('<span class="label label-important">Cancelled</span><br/><span class="label label-important">' + reservation.cancelled.format("YYYY-MM-DD HH:mm:ss") + '</span>');
    		cancelledLabel.popover({
    			placement : 'top',
				title   : 'Cancelled Reservation',
				content : '<p>This reservation has been <b>cancelled</b>.</p><p>A reservation ends '+
				 'either because the end of the reservaiton time span has been reached or because it has '+
				 'been cancelled. Due to network delays or temporary network disconnections in the testbed '+
				 'backend node outputs might still arrive delayed. After a couple of minutes after the '+
				 'last delayed node output was received the reservation is finalized, i.e. it is '+
				 'guaranteed that there will not be any more outputs attached to the reservations log.</p>'
			});
		}

		if (reservation.cancelled) {
			rowData.push(cancelledLabel);
		} else {
			rowData.push('&nbsp;');
		}

		return rowData;
	};

	var preFilterFun = null;
	var preSelectFun = null;
	var showCheckBoxes = false;
	var showFilterBox = false;
	var options = {
		noDataMessage    : 'No reservations found.',
		sortColumn       : 0,
		pagination       : true,
		paginationAmount : 10
	};

	var table = new WiseGuiTable(model, headers, rowProducer, preFilterFun, preSelectFun, showCheckBoxes, showFilterBox, options);

	parent.empty();
	parent.append(table.html);
};

global.buildReservationTable = function(parent) {
	
	var pills = $(
			'<div class="tabbable">' +
			'	<ul class="nav nav-pills">' +
			'		<li class="active"><a href="#WiseGuiPublicReservationsCurrentFuture">Current and Upcoming</a></li>' +
			'		<li               ><a href="#WiseGuiPublicReservationsPast">Past</a></li>' +
			'	</ul>' +
			'	<div class="tab-content">' +
			'		<div class="tab-pane active" id="WiseGuiPublicReservationsCurrentFuture"></div>' +
			'		<div class="tab-pane"        id="WiseGuiPublicReservationsPast"></div>' +
			'	</div>' +
			'</div>'
	);

	var currentAndFutureDiv = pills.find('#WiseGuiPublicReservationsCurrentFuture');
	var pastDiv = pills.find('#WiseGuiPublicReservationsPast');

	var loadPublicReservations = function(past, showCancelled) {
		wisebed.reservations.getPublic(
			!past ? moment() : null,
			 past ? moment() : null,
			function(reservations) {
				buildReservationTableInternal(!past ? currentAndFutureDiv : pastDiv, reservations);
			},
			WiseGui.showAjaxError,
			showCancelled
		);
	};

	pills.find('a[href="#WiseGuiPublicReservationsCurrentFuture"]').click(function(e) {
		e.preventDefault();
		loadPublicReservations(false, false);
		$(this).tab('show');
	});

	pills.find('a[href="#WiseGuiPublicReservationsPast"]').click(function(e) {
		e.preventDefault();
		loadPublicReservations(true, true);
		$(this).tab('show');
	});

	loadPublicReservations(false, false);
	
	parent.empty();
	parent.append(pills);
};

global.loadReservationViewContainer = function(navigationData, parentDiv) {

	wisebed.reservations.getByExperimentId(navigationData.experimentId, function(reservation) {

		var reservationView = new WiseGuiReservationView(reservation);
		parentDiv.append(reservationView.view);
		$(window).trigger('hashchange');

	}, WiseGui.showAjaxError);
};

global.getNavigationKey = function(navigationData) {
	if (navigationData.nav == 'overview' && navigationData.experimentId === '') {
		return 'overview';
	} else if (navigationData.nav == 'experiment' && navigationData.experimentId !== '') {
		return 'experimentId=' + navigationData.experimentId;
	}
	return undefined;
};

global.getCreateContentFunction = function(navigationData) {
	if (navigationData.nav == 'overview' && navigationData.experimentId === '')   { return loadTestbedDetailsContainer;  }
	if (navigationData.nav == 'experiment' && navigationData.experimentId !== '') { return loadReservationViewContainer; }
	return undefined;
};

global.showReservationsDialog = function() {
	var existingDialog = $("#WiseGuiReservationDialog");
	if (existingDialog.length !== 0) {existingDialog.modal('show');}
	else {new WiseGuiReservationDialog();}
};

global.navigateToNavigationData = function(navigationData) {
	console.log('navigateToNavigationData(%s)', JSON.stringify(navigationData));
	if (navigationData) {
		window.location.hash = '#nav='+encodeURIComponent(navigationData.nav)+'&experimentId='+encodeURIComponent(navigationData.experimentId)+'&tab='+encodeURIComponent(navigationData.tab);
	} else {
		window.location.hash = '#nav=overview';
	}
};

global.navigateTo = function(experimentId, tab) {
	navigateToNavigationData({
		nav          : (experimentId ? 'experiment' : 'overview'),
		experimentId : (experimentId || ''),
		tab          : (tab || '')
	});
};

global.getNavigationData = function(fragment) {
	var decoded = decodeUrlHash();
	return {
		nav          : decoded.nav          || 'overview',
		experimentId : decoded.experimentId || '',
		tab          : decoded.tab          || ''
	};
};

global.createNavigationContainer = function() {

	var container = $('<div class="WiseGuiNavigationContainer" id="WiseGuiNavigationContainer"/>');
	
	$('#WiseGuiContainer').prepend(container);

	var navigationViewer = new WiseGuiNavigationView();
	container.append(navigationViewer.view);
	return container;
};

global.switchContentContainer = function(navigationData, navigationKey) {
	$('#WiseGuiContainer .WiseGuiContentContainer').hide();
	getOrCreateContentContainer(navigationData, navigationKey).show();
};

global.getOrCreateContentContainer = function(navigationData, navigationKey) {
	
	if (contentContainers[navigationKey]) {
		return contentContainers[navigationKey];
	}

	var container = createContentContainer(navigationData);
	contentContainers[navigationKey] = container;
	return container;
};

global.createContentContainer = function(navigationData) {
	
	var container = $('<div class="WiseGuiContentContainer"/>');
	container.hide();

	$('#WiseGuiContainer').append(container);

	var createContentFunction = getCreateContentFunction(navigationData);
	if (createContentFunction === undefined) {
		console.warn("createContentFunction is undefined");
		console.warn(navigationData);
	} else {
		createContentFunction(navigationData, container);
	}

	return container;
};

global.connectEventWebSocket = function() {

	console.log('EventWebSocket: trying to connect');

	var onOpen = function() {
		
		console.log('EventWebSocket: connection established');
		eventWebSocketState = 'connected';
		$(window).trigger(EVENT_EVENTWEBSOCKET_CONNECTED);

		if (eventWebSocketSchedule !== undefined) {
			console.log('EventWebSocket: cancelling reconnection schedule');
			window.clearInterval(eventWebSocketSchedule);
			eventWebSocketSchedule = undefined;
		}
	};

	var onClosed = function(closeEvent) {
		
		console.log(eventWebSocket.readyState);
		console.log(closeEvent);

		if (eventWebSocketState == 'connected') {
			console.log('EventWebSocket: connection closed');
			$(window).trigger(EVENT_EVENTWEBSOCKET_DISCONNECTED);
			eventWebSocketState = 'disconnected';
		}

		if (eventWebSocketSchedule === undefined) {
			console.log('EventWebSocket: scheduling reconnect every 5 seconds');
			eventWebSocketSchedule = window.setInterval(connectEventWebSocket, 5000);
		}
	};

	var onAttached = function(devicesAttachedEvent) { $(window).trigger(EVENT_DEVICES_ATTACHED, devicesAttachedEvent); };
	var onDetached = function(devicesDetachedEvent) { $(window).trigger(EVENT_DEVICES_DETACHED, devicesDetachedEvent); };

	eventWebSocket = new wisebed.EventWebSocket(onAttached, onDetached, onOpen, onClosed);

	$(window).bind(EVENT_EVENTWEBSOCKET_DISCONNECTED, function() {

		var disconnectionModal = $(
				'<div class="modal hide" id="WiseGuiDisconnectionModal">' +
				'	<div class="modal-header">' +
				'		<button type="button" class="close" data-dismiss="modal">×</button>' +
				'		<h3>Connection Lost...</h3>' +
				'	</div>' +
				'	<div class="modal-body">' +
				'		<p>WiseGui is currently offline as it lost the connection to the testbeds portal server. Trying to reconnect every 5 seconds...</p>' +
				'	</div>' +
				'</div>'
		);
		
		$(document.body).append(disconnectionModal);
		disconnectionModal.modal('show');

		$(window).bind(EVENT_EVENTWEBSOCKET_CONNECTED, function() {
			$('#WiseGuiDisconnectionModal').modal('hide');
			$('#WiseGuiDisconnectionModal').remove();
		});
	});

};

// export event constants into global namespace for backwards compatibility with older code
for (var eventName in WiseGuiEvents) {
	global[eventName] = WiseGuiEvents[eventName];
}

// print all events to console for debugging as soon as they occur
for (var eventName in WiseGuiEvents) {
	/*jshint loopfunc:true */
	$(window).bind(WiseGuiEvents[eventName], function(e, data) {
		console.log('+++ %s', e.type);
	});
}

// some more global vars for backwards compatibility
global.wisebed                            = new wjs.Wisebed(wisebedBaseUrl, wisebedWebSocketBaseUrl);
global.WisebedPublicReservationData       = wjs.WisebedPublicReservationData;
global.WisebedConfidentialReservationData = wjs.WisebedConfidentialReservationData;
global.WisebedReservation                 = wjs.WisebedReservation;

global.navigationContainer    = null;
global.contentContainers      = {};
global.loginObserver          = new WiseGuiLoginObserver();
global.reservationObserver    = new WiseGuiReservationObserver();
global.testbedDescription     = null;
global.eventWebSocket         = null;
global.eventWebSocketSchedule = null;
global.eventWebSocketState    = 'disconnected';
global.isLoggedIn             = false;
global.$                      = $;

global.decodeUrlHash = function () {
	var urlParams = {};
    var match,
        pl     = /\+/g,  // Regex for replacing addition symbol with a space
        search = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
        query  = window.location.hash.substring(1);

    while ((match = search.exec(query)) !== null) {
       urlParams[decode(match[1])] = decode(match[2]);
    }

    return urlParams;
};

// extend jQuery with the 'exists' function to check if selector returned empty result
// and only do so if not done yet...
if (!($.fn.exists)) {
	$.fn.exists = function () {
		return this.length !== 0;
	};
}

$(function () {

	$('.modal').modal({ keyboard: true });

	$(window).bind(EVENT_LOGGED_IN,  function() { isLoggedIn = true;  });
	$(window).bind(EVENT_LOGGED_OUT, function() { isLoggedIn = false; });

	connectEventWebSocket();

	wisebed.getTestbedDescription(
		function(td) {

			testbedDescription = td;

			navigationContainer = createNavigationContainer();

			var navigationData = {nav:'overview', experimentId:'', tab:''};
			getOrCreateContentContainer(navigationData, getNavigationKey(navigationData));

			reservationObserver.startObserving();
			loginObserver.startObserving();

			window.onhashchange = function(e) {
				
				console.log('onhashchange');
				e.preventDefault();

				var navigationData = getNavigationData();
				var navigationKey  = getNavigationKey(navigationData);
				switchContentContainer(navigationData, navigationKey);
				$(window).trigger(WiseGuiEvents.EVENT_NAVIGATION, navigationData);
			};
			
			$(window).trigger('hashchange');

			// Test for 3rd party cookies
			var cookieCallbackError = function(jqXHR, textStatus, errorThrown) {
				WiseGui.showErrorAlert(
						'Your browser doesn\'t support 3rd party cookies. ' +
						'Please enable them or you will not be able to login. ' +
						'Otherwise you can go to <a href="' + wisebedBaseUrl + '">' + wisebedBaseUrl + '</a>'
				);
			};
			wisebed.testCookie(function() {}, cookieCallbackError);
		},
		WiseGui.showAjaxError
	);
});

}).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"./wisegui-events.js":16,"./wisegui-google-maps-view.js":18,"./wisegui-login-observer.js":20,"./wisegui-modal-dialog.js":21,"./wisegui-navigation-view.js":22,"./wisegui-node-table.js":24,"./wisegui-reservation-dialog.js":30,"./wisegui-reservation-observer.js":31,"./wisegui-reservation-view.js":32,"./wisegui-table.js":36,"toastr":6,"underscore":7,"wisebed.js":13}]},{},[37]);
