package io.smallrye.faulttolerance.tck;

import org.jboss.logging.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class LoggingTestListener implements ITestListener {
    private static final Logger LOG = Logger.getLogger(LoggingTestListener.class.getPackage().getName());

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("Starting " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("Succeeded " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.info("Failed " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.info("Skipped " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }
}
