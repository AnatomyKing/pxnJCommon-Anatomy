package com.poixson.commonjava.utils.utilsCrypt;

import org.junit.Assert;
import org.junit.Test;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsCrypt;
import com.poixson.commonjava.xLogger.xLogTest;


public class base64Test {

	private static final String TEST_NAME_ENCODE = "Base64 Encode";
	private static final String TEST_NAME_DECODE = "Base64 Decode";

	private static final String TEST_DECODED = "This is the test data! This is the test data!";
	private static final String TEST_ENCODED = "VGhpcyBpcyB0aGUgdGVzdCBkYXRhISBUaGlzIGlzIHRoZSB0ZXN0IGRhdGEh";



	// base64 encode
	@Test
	public void EncodeBase64Test() {
		xLogTest.testStart(TEST_NAME_ENCODE);
		xLogTest.publish("Input:  "+TEST_DECODED);
		final String data = utilsCrypt.Base64Encode(TEST_DECODED);
		xLogTest.publish("Output: "+data);
		Assert.assertTrue(utils.notEmpty(data));
		Assert.assertEquals(TEST_ENCODED, data);
		xLogTest.testPassed(TEST_NAME_ENCODE);
	}
	// base64 decode
	@Test
	public void DecodeBase64Test() {
		xLogTest.testStart(TEST_NAME_DECODE);
		xLogTest.publish("Input:  "+TEST_ENCODED);
		final String data = utilsCrypt.Base64Decode(TEST_ENCODED);
		xLogTest.publish("Output: "+data);
		Assert.assertTrue(utils.notEmpty(data));
		Assert.assertEquals(TEST_DECODED, data);
		xLogTest.testPassed(TEST_NAME_DECODE);
	}



}
