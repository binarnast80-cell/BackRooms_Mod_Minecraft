const puppeteer = require('puppeteer');

(async () => {
    console.log('⏰ Запускаем невидимый браузер...');
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    
    await page.setViewport({ width: 1920, height: 1080, deviceScaleFactor: 2 });
    
    console.log('🌐 Открываем презентацию...');
    await page.goto('http://127.0.0.1:8080/', { waitUntil: 'networkidle0', timeout: 60000 });

    await page.evaluate(() => {
        const contents = document.querySelectorAll('.slide-content');
        contents.forEach(el => {
            el.style.setProperty('animation', 'none', 'important');
            el.style.setProperty('opacity', '1', 'important');
            el.style.setProperty('transform', 'none', 'important');
        });
        document.documentElement.style.setProperty('scroll-snap-type', 'none', 'important');
        document.body.style.setProperty('scroll-snap-type', 'none', 'important');
        document.documentElement.style.setProperty('overflow', 'visible', 'important');
        document.body.style.setProperty('overflow', 'visible', 'important');
    });

    await page.addStyleTag({content: `
        @page {
            size: 1920px 1080px;
            margin: 0;
        }
        @media print {
            body, html {
                height: auto !important;
                overflow: visible !important;
                scroll-snap-type: none !important;
                background: #0a2e2a !important;
                -webkit-print-color-adjust: exact !important;
                print-color-adjust: exact !important;
            }
            .slide {
                height: 1080px !important;
                min-height: 1080px !important;
                width: 1920px !important;
                page-break-after: always !important;
                page-break-inside: avoid !important;
                break-after: page !important;
                break-inside: avoid !important;
                display: flex !important;
                align-items: center !important;
                justify-content: center !important;
                position: relative !important;
                box-sizing: border-box !important;
            }
            .nav-dots, .slide-counter {
                display: none !important;
            }
            * {
                animation: none !important;
                transition: none !important;
            }
        }
    `});

    await new Promise(r => setTimeout(r, 2000));

    console.log('�Ҥ� Генерируем идеальный векторный PDF...');
    await page.pdf({
        path: 'Backrooms_Presentation_Vector.pdf',
        width: '1920px',
        height: '1080px',
        printBackground: true,
        pageRanges: '1-9'
    });

    await browser.close();
    console.log('✅ Готово! Файл сохранен.');
})();