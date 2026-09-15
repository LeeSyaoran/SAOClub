const { chromium } = require('playwright');
const path = require('path');

const SCREENSHOTS_DIR = __dirname;
const BASE_URL = 'http://localhost:5173';

async function capturePage(browser, url, filename, waitSelector = null) {
    const filepath = path.join(SCREENSHOTS_DIR, filename);
    console.log(`📸 Capturing: ${filename}`);
    
    try {
        const page = await browser.newPage();
        await page.setViewportSize({ width: 1920, height: 1080 });
        
        await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
        
        if (waitSelector) {
            await page.waitForSelector(waitSelector, { timeout: 10000 }).catch(() => {});
        }
        
        await page.screenshot({ 
            path: filepath, 
            fullPage: false 
        });
        
        console.log(`✅ Saved: ${filename}`);
        await page.close();
        return true;
    } catch (err) {
        console.log(`❌ Failed: ${filename} - ${err.message}`);
        return false;
    }
}

async function loginAsAdmin(browser) {
    console.log('🔐 Logging in as admin...');
    const page = await browser.newPage();
    await page.setViewportSize({ width: 1920, height: 1080 });
    
    try {
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
        await page.waitForSelector('input[type="text"]', { timeout: 5000 });
        
        // Find login form and fill
        const usernameInput = await page.locator('input[type="text"], input[type="email"]').first();
        const passwordInput = await page.locator('input[type="password"]').first();
        
        await usernameInput.fill('admin');
        await passwordInput.fill('admin123');
        
        // Submit
        await page.click('button[type="submit"], button:has-text("Đăng nhập"), button:has-text("Login")');
        await page.waitForTimeout(3000);
        
        console.log('✅ Logged in as admin');
        await page.close();
        return true;
    } catch (err) {
        console.log(`⚠️ Login failed: ${err.message}`);
        await page.close();
        return false;
    }
}

async function main() {
    console.log('╔═══════════════════════════════════════════════════════════╗');
    console.log('║     SAOClub Screenshot Capture Tool                      ║');
    console.log('╚═══════════════════════════════════════════════════════════╝\n');
    
    let browser;
    try {
        browser = await chromium.launch({ headless: true });
        
        // Login first
        await loginAsAdmin(browser);
        
        // Customer pages (no login needed)
        await capturePage(browser, `${BASE_URL}/`, '01-trang-chu.png');
        await capturePage(browser, `${BASE_URL}/products`, '02-danh-sach-san-pham.png');
        
        // Try to get product detail page
        const page1 = await browser.newPage();
        await page1.goto(`${BASE_URL}/products`, { waitUntil: 'networkidle' });
        const firstProduct = page1.locator('a[href*="/products/"]').first();
        if (await firstProduct.count() > 0) {
            await firstProduct.click();
            await page1.waitForTimeout(2000);
            await page1.screenshot({ path: path.join(SCREENSHOTS_DIR, '03-chi-tiet-san-pham.png') });
            console.log('✅ Saved: 03-chi-tiet-san-pham.png');
        }
        await page1.close();
        
        await capturePage(browser, `${BASE_URL}/cart`, '05-gio-hang.png');
        
        // Admin pages (need login)
        await capturePage(browser, `${BASE_URL}/admin`, '10-admin-dashboard.png');
        await capturePage(browser, `${BASE_URL}/admin/products`, '11-admin-san-pham.png');
        await capturePage(browser, `${BASE_URL}/admin/orders`, '13-admin-don-hang.png');
        await capturePage(browser, `${BASE_URL}/admin/pos`, '14-admin-pos.png');
        await capturePage(browser, `${BASE_URL}/admin/inventory`, '16-admin-ton-kho.png');
        await capturePage(browser, `${BASE_URL}/admin/warehouse`, '17-admin-nhap-kho.png');
        
        // Login page (new context)
        const loginPage = await browser.newPage();
        await loginPage.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
        await loginPage.screenshot({ path: path.join(SCREENSHOTS_DIR, '09-dang-nhap.png') });
        console.log('✅ Saved: 09-dang-nhap.png');
        await loginPage.close();
        
        await browser.close();
        
        console.log('\n╔═══════════════════════════════════════════════════════════╗');
        console.log('║  ✅ Screenshot capture complete!                          ║');
        console.log('║  Check the screenshots folder for images                ║');
        console.log('╚═══════════════════════════════════════════════════════════╝');
        
    } catch (err) {
        console.log(`\n❌ Error: ${err.message}`);
        if (browser) await browser.close();
    }
}

main();
