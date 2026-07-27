document.addEventListener('DOMContentLoaded', function () {
    const cryptoSelect = document.getElementById('crypto-select');
    const walletAddress = document.getElementById('wallet-address');
    const amountInput = document.getElementById('amount');
    const memoInput = document.getElementById('memo');
    const memoGroup = document.getElementById('memo-group');
    const generateBtn = document.getElementById('generate-btn');
    const downloadBtn = document.getElementById('download-btn');
    const qrContainer = document.getElementById('qrcode');
    const qrPlaceholder = document.getElementById('qr-placeholder');
    const paymentInfo = document.getElementById('payment-info');
    const infoCrypto = document.getElementById('info-crypto');
    const infoAddress = document.getElementById('info-address');
    const infoAmount = document.getElementById('info-amount');
    const infoAmountRow = document.getElementById('info-amount-row');
    const addressHint = document.getElementById('address-hint');

    let currentQR = null;

    const addressHints = {
        bitcoin: '例如：bc1q... 或 1... 或 3...',
        ethereum: '例如：0x...',
        litecoin: '例如：L... 或 M... 或 ltc1...',
        bitcoincash: '例如：bitcoincash:... 或 q...',
        dogecoin: '例如：D...',
        usdt_trc20: '例如：T...（波场地址）',
        usdt_erc20: '例如：0x...（以太坊地址）',
        usdt_bep20: '例如：0x...（BSC地址）',
        trx: '例如：T...（波场地址）',
        bnb_bep20: '例如：0x...（BSC地址）',
        solana: '例如：...（Base58 地址）',
        ripple: '例如：r...（可选填写 Memo）'
    };

    cryptoSelect.addEventListener('change', function () {
        const selected = cryptoSelect.options[cryptoSelect.selectedIndex];
        const value = selected.value;
        addressHint.textContent = addressHints[value] || '请输入正确的钱包地址';

        // XRP 显示 Memo 输入框
        if (value === 'ripple') {
            memoGroup.style.display = 'flex';
        } else {
            memoGroup.style.display = 'none';
            memoInput.value = '';
        }
    });

    function buildPaymentURI() {
        const selected = cryptoSelect.options[cryptoSelect.selectedIndex];
        const scheme = selected.dataset.scheme;
        const symbol = selected.dataset.symbol;
        const network = selected.dataset.network;
        const address = walletAddress.value.trim();
        const amount = amountInput.value.trim();
        const memo = memoInput.value.trim();

        if (!address) {
            alert('请先输入钱包地址');
            return null;
        }

        let uri = '';

        // 根据协议构建 URI
        if (scheme === 'bitcoin' || scheme === 'litecoin' || scheme === 'bitcoincash' || scheme === 'dogecoin') {
            uri = `${scheme}:${address}`;
            if (amount && parseFloat(amount) > 0) {
                uri += `?amount=${amount}`;
            }
        } else if (scheme === 'ethereum' || scheme === 'bsc') {
            uri = `${scheme}:${address}`;
            const params = [];
            if (amount && parseFloat(amount) > 0) {
                params.push(`value=${amount}`);
            }
            // ERC20 / BEP20 USDT 转账需要合约交互，普通收款只用地址更安全
            if (params.length > 0) {
                uri += '?' + params.join('&');
            }
        } else if (scheme === 'tron') {
            uri = `${scheme}:${address}`;
            if (amount && parseFloat(amount) > 0) {
                uri += `?amount=${amount}`;
            }
        } else if (scheme === 'solana') {
            uri = `${scheme}:${address}`;
            if (amount && parseFloat(amount) > 0) {
                uri += `?amount=${amount}`;
            }
        } else if (scheme === 'ripple') {
            uri = `${scheme}:${address}`;
            const params = [];
            if (amount && parseFloat(amount) > 0) {
                params.push(`amount=${amount}`);
            }
            if (memo) {
                params.push(`dt=${encodeURIComponent(memo)}`);
            }
            if (params.length > 0) {
                uri += '?' + params.join('&');
            }
        }

        return {
            uri,
            address,
            amount,
            memo,
            symbol,
            network,
            label: selected.text
        };
    }

    function generateQR() {
        const data = buildPaymentURI();
        if (!data) return;

        qrContainer.innerHTML = '';
        qrContainer.style.display = 'block';
        qrPlaceholder.style.display = 'none';

        currentQR = new QRCode(qrContainer, {
            text: data.uri,
            width: 220,
            height: 220,
            colorDark: '#000000',
            colorLight: '#ffffff',
            correctLevel: QRCode.CorrectLevel.M
        });

        infoCrypto.textContent = `${data.label} · ${data.network}`;
        infoAddress.textContent = data.address;

        if (data.amount && parseFloat(data.amount) > 0) {
            infoAmount.textContent = `${data.amount} ${data.symbol}`;
            infoAmountRow.style.display = 'flex';
        } else {
            infoAmountRow.style.display = 'none';
        }

        paymentInfo.style.display = 'block';
        downloadBtn.disabled = false;
    }

    function downloadQR() {
        const img = qrContainer.querySelector('img');
        if (!img) return;

        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        canvas.width = 240;
        canvas.height = 240;

        // 绘制白色背景
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        const qrImg = new Image();
        qrImg.crossOrigin = 'anonymous';
        qrImg.onload = function () {
            ctx.drawImage(qrImg, 10, 10, 220, 220);

            const link = document.createElement('a');
            const selected = cryptoSelect.options[cryptoSelect.selectedIndex];
            const symbol = selected.dataset.symbol.toLowerCase();
            link.download = `crypto-qr-${symbol}-${Date.now()}.png`;
            link.href = canvas.toDataURL('image/png');
            link.click();
        };
        qrImg.src = img.src;
    }

    generateBtn.addEventListener('click', generateQR);
    downloadBtn.addEventListener('click', downloadQR);

    // 回车生成
    [walletAddress, amountInput, memoInput].forEach(input => {
        input.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                generateQR();
            }
        });
    });
});
