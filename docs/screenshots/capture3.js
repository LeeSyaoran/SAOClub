const { chromium } = require('playwright');
const path = require('path');

const SCREENSHOTS_DIR = __dirname;
const BASE_URL = 'http://localhost:5173';

async function capturePage(browser, url, filename) {
    const filepath = path.join(SCREENSHOTS_DIR, filename);
    console.log(`📸 Capturing: ${filename}`);
    
    try {
        const page = await browser.newPage();
        await page.setViewportSize({ width: 1920, height: 1080 });
        
        await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
        await page.waitForTimeout(2000);
        
        await page.screenshot({ path: filepath, fullPage: false });
        
        console.log(`✅ Saved: ${filename}`);
        await page.close();
        return true;
    } catch (err) {
        console.log(`❌ Failed: ${filename} - ${err.message}`);
        return false;
    }
}

async function captureModal(browser, url, modalSelector, filename) {
    const filepath = path.join(SCREENSHOTS_DIR, filename);
    console.log(`📸 Capturing modal: ${filename}`);
    
    try {
        const page = await browser.newPage();
        await page.setViewportSize({ width: 1920, height: 1080 });
        
        await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
        
        // Try to open modal by clicking a button
        if (modalSelector) {
            await page.click(modalSelector).catch(() => {
                console.log('⚠️ Modal selector not found, taking full page screenshot');
            });
            await page.waitForTimeout(1500);
        }
        
        await page.screenshot({ path: filepath, fullPage: false });
        
        console.log(`✅ Saved: ${filename}`);
        await page.close();
        return true;
    } catch (err) {
        console.log(`❌ Failed: ${filename} - ${err.message}`);
        return false;
    }
}

async function main() {
    console.log('\n📸 Phase 3: Capturing remaining pages...\n');
    
    let browser;
    try {
        browser = await chromium.launch({ headless: true });
        
        // Try to get product detail - try common product IDs
        const productIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        let found = false;
        
        for (const id of productIds) {
            const page = await browser.newPage();
            await page.setViewportSize({ width: 1920, height: 1080 });
            
            try {
                await page.goto(`${BASE_URL}/products/${id}`, { 
                    waitUntil: 'networkidle', 
                    timeout: 10000 
                });
                
                // Check if page loaded a valid product
                const content = await page.content();
                if (content.includes('chi-tiet') || content.includes('product') && !content.includes('error')) {
                    await page.screenshot({ 
                        path: path.join(SCREENSHOTS_DIR, '03-chi-tiet-san-pham.png'),
                        fullPage: false
                    });
                    console.log(`✅ Saved: 03-chi-tiet-san-pham.png (product ID: ${id})`);
                    found = true;
                    await page.close();
                    break;
                }
            } catch (e) {}
            await page.close();
        }
        
        if (!found) {
            // Take screenshot of homepage if no product found
            await capturePage(browser, `${BASE_URL}/`, '03-chi-tiet-san-pham.png');
        }
        
        // Checkout page
        await capturePage(browser, `${BASE_URL}/checkout`, '06-dat-hang.png');
        
        // Admin pages - try to open modals
        await captureModal(browser, `${BASE_URL}/admin/products`, 'button:has-text("Thêm"), button:has-text("Add"), [class*="btn-primary"]', '12-admin-them-san-pham.png');
        
        // Try to capture serial selection modal (from POS)
        await captureModal(browser, `${BASE_URL}/admin/pos`, '[class*="serial"], [class*="modal"]', '15-admin-chon-serial.png');
        
        // Product comparison - try to open from homepage
        await capturePage(browser, `${BASE_URL}/products/compare`, '04-so-sanh-san-pham.png');
        
        await browser.close();
        
        console.log('\n✅ Phase 3 complete!');
        
    } catch (err) {
        console.log(`\n❌ Error: ${err.message}`);
        if (browser) await browser.close();
    }
}

main();
