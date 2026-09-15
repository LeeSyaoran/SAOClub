const { chromium } = require('playwright');
const path = require('path');

const SCREENSHOTS_DIR = __dirname;
const BASE_URL = 'http://localhost:5173';

async function capturePage(browser, url, filename, waitMs = 2000) {
    const filepath = path.join(SCREENSHOTS_DIR, filename);
    console.log(`📸 Capturing: ${filename}`);
    
    try {
        const page = await browser.newPage();
        await page.setViewportSize({ width: 1920, height: 1080 });
        
        await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
        await page.waitForTimeout(waitMs);
        
        await page.screenshot({ path: filepath, fullPage: false });
        
        console.log(`✅ Saved: ${filename}`);
        await page.close();
        return true;
    } catch (err) {
        console.log(`❌ Failed: ${filename} - ${err.message}`);
        return false;
    }
}

async function captureProductDetail(browser) {
    console.log('📸 Capturing: 03-chi-tiet-san-pham.png');
    
    try {
        const page = await browser.newPage();
        await page.setViewportSize({ width: 1920, height: 1080 });
        
        await page.goto(`${BASE_URL}/products`, { waitUntil: 'networkidle' });
        
        // Click first product card
        const productLinks = page.locator('.product-card a, .card a[href*="products"], a[href*="/products/"]');
        const count = await productLinks.count();
        
        if (count > 0) {
            await productLinks.first().click();
            await page.waitForTimeout(3000);
            await page.screenshot({ 
                path: path.join(SCREENSHOTS_DIR, '03-chi-tiet-san-pham.png'),
                fullPage: false
            });
            console.log('✅ Saved: 03-chi-tiet-san-pham.png');
        } else {
            console.log('⚠️ No product links found');
        }
        
        await page.close();
    } catch (err) {
        console.log(`❌ Failed: 03-chi-tiet-san-pham.png - ${err.message}`);
    }
}

async function main() {
    console.log('\n📸 Phase 2: Capturing remaining pages...\n');
    
    let browser;
    try {
        browser = await chromium.launch({ headless: true });
        
        // Product detail
        await captureProductDetail(browser);
        
        // Customer account pages (need login)
        await capturePage(browser, `${BASE_URL}/account/orders`, '07-tai-khoan-don-hang.png');
        await capturePage(browser, `${BASE_URL}/account/points`, '08-tich-diem-vong-quay.png');
        
        // Admin pages
        await capturePage(browser, `${BASE_URL}/admin/customers`, '21-admin-khach-hang.png');
        await capturePage(browser, `${BASE_URL}/admin/returns`, '18-admin-tra-hang.png');
        await capturePage(browser, `${BASE_URL}/admin/warranty`, '19-admin-bao-hanh.png');
        await capturePage(browser, `${BASE_URL}/admin/promotions`, '20-admin-khuyen-mai.png');
        await capturePage(browser, `${BASE_URL}/admin/staff`, '22-admin-nhan-vien.png');
        await capturePage(browser, `${BASE_URL}/admin/reports`, '23-admin-bao-cao.png');
        await capturePage(browser, `${BASE_URL}/admin/settings`, '24-admin-cai-dat.png');
        
        await browser.close();
        
        console.log('\n✅ Phase 2 complete!');
        
    } catch (err) {
        console.log(`\n❌ Error: ${err.message}`);
        if (browser) await browser.close();
    }
}

main();
