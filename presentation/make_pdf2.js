const puppeteer = require('puppeteer');
const { PDFDocument } = require('pdf-lib');
const fs = require('fs');

(async () => {
    console.log('⏳ Запускаем невидимый браузер...');
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    
    // Экран 16:9 для скриншотов
    await page.setViewport({ width: 1920, height: 1080, deviceScaleFactor: 2 });
    
    console.log('🌐 Открываем презентацию...');
    await page.goto('http://localhost:8080/', { waitUntil: 'networkidle0', timeout: 60000 });

    // Отключаем скролл-снэп и анимации для стабильности
    await page.evaluate(() => {
        // Убиваем Intersection Observer, если он мешает
        if (window.observer) window.observer.disconnect();
        
        document.documentElement.style.setProperty('scroll-snap-type', 'none', 'important');
        document.body.style.setProperty('scroll-snap-type', 'none', 'important');
        
        const contents = document.querySelectorAll('.slide-content');
        contents.forEach(el => {
            el.style.setProperty('animation', 'none', 'important');
            el.style.setProperty('opacity', '1', 'important');
            el.style.setProperty('transform', 'none', 'important');
        });
    });

    console.log('📸 Начинаем генерацию из скриншотов (100% точность)...');
    
    // Создаем пустой PDF-документ
    const pdfDoc = await PDFDocument.create();

    const slideCount = 14;
    
    for (let i = 0; i < slideCount; i++) {
        // Скроллим точно до начала нужного слайда
        await page.evaluate((idx) => {
            // Отключаем плавный скролл, чтобы браузер прыгал моментально
            document.documentElement.style.setProperty('scroll-behavior', 'auto', 'important');
            document.body.style.setProperty('scroll-behavior', 'auto', 'important');
            
            // Находим нужный слайд и прыгаем к нему
            const slide = document.querySelectorAll('.slide')[idx];
            if (slide) {
                slide.scrollIntoView({ behavior: 'instant', block: 'start' });
            }
            
            // Если на странице есть точки навигации, обновляем их чисто для визуальной точности
            const dots = document.querySelectorAll('.nav-dot');
            dots.forEach(d => d.classList.remove('active'));
            if (dots[idx]) dots[idx].classList.add('active');
            
            // Обновляем счетчик
            const counter = document.getElementById('slideCounter');
            if (counter) counter.textContent = `${String(idx + 1).padStart(2, '0')} / 14`;
        }, i);

        // Даем время на отрисовку скролла
        await new Promise(r => setTimeout(r, 300));

        // Делаем скриншот видимой области
        const screenshotBuffer = await page.screenshot({ type: 'png' });
        
        // Вставляем скриншот на новую страницу PDF
        const pdfImage = await pdfDoc.embedPng(screenshotBuffer);
        const pagePdf = pdfDoc.addPage([1920, 1080]);
        pagePdf.drawImage(pdfImage, {
            x: 0,
            y: 0,
            width: 1920,
            height: 1080,
        });
        
        process.stdout.write(`✅ Слайд ${i + 1} / 14 готов\r`);
    }

    console.log('\n💾 Сохраняем файл...');
    const pdfBytes = await pdfDoc.save();
    fs.writeFileSync('Kundelik_Presentation.pdf', pdfBytes);

    await browser.close();
    console.log('🎉 Готово! Проверьте файл: Kundelik_Presentation.pdf');
})();
