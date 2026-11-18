document.addEventListener('DOMContentLoaded', () => {
  const menuBtn = document.getElementById('menuToggle');
  const navLinks = document.getElementById('navLinks');
  if (menuBtn && navLinks) {
    menuBtn.addEventListener('click', () => {
      navLinks.classList.toggle('open');
    });
  }

  let demoTickets = 18;
  let demoPts = 12450;
  const ticketText = document.getElementById('demoTicketCount');
  const ticketFill = document.getElementById('demoTicketFill');
  const walletText = document.getElementById('demoWalletPts');
  const demoBtn = document.getElementById('demoPlayTicketBtn');

  if (demoBtn && ticketText && ticketFill && walletText) {
    demoBtn.addEventListener('click', () => {
      if (demoTickets < 30) {
        demoTickets += 1;
      } else {
        demoTickets = 0;
        demoPts += 1500;
      }
      const pct = Math.min(100, Math.round((demoTickets / 30) * 100));
      ticketText.textContent = `${demoTickets} / 30 Tickets`;
      ticketFill.style.width = `${pct}%`;
      walletText.textContent = `${demoPts.toLocaleString()} pts`;
      demoBtn.textContent = demoTickets >= 30
        ? 'Claim +1,500 Mega Pts!'
        : '+1 Ticket Added (Tap to Simulate)';
    });
  }

  const megaSlider = document.getElementById('calcMegaOffers');
  const normalSlider = document.getElementById('calcNormalOffers');
  const surveySlider = document.getElementById('calcSurveys');
  const referSlider = document.getElementById('calcReferrals');

  const updateCalculator = () => {
    if (!megaSlider || !normalSlider || !surveySlider || !referSlider) return;

    const megaCount = Number(megaSlider.value);
    const normalCount = Number(normalSlider.value);
    const surveyCount = Number(surveySlider.value);
    const referCount = Number(referSlider.value);

    const cappedNormal = Math.min(normalCount, Math.max(0, 30 - megaCount));
    document.getElementById('valMegaOffers').textContent = `${megaCount} / day`;
    document.getElementById('valNormalOffers').textContent = `${cappedNormal} / day`;
    document.getElementById('valSurveys').textContent = `${surveyCount} / day`;
    document.getElementById('valReferrals').textContent = `${referCount} / month`;

    const dailyPts = (megaCount * 1500) + (cappedNormal * 800) + (surveyCount * 450);
    const monthlyOfferPts = dailyPts * 30;
    const monthlyReferPts = referCount * 700;
    const totalMonthlyPts = monthlyOfferPts + monthlyReferPts;

    const userInr = totalMonthlyPts / 100;
    const grossInr = userInr / 0.70;
    const platformInr = grossInr * 0.30;

    document.getElementById('outDailyPts').textContent = `${dailyPts.toLocaleString()} pts`;
    document.getElementById('outMonthlyPts').textContent = `${totalMonthlyPts.toLocaleString()} pts`;
    document.getElementById('outUserInr').textContent = `₹${userInr.toFixed(2)}`;
    document.getElementById('outAdminSplit').textContent = `₹${platformInr.toFixed(2)}`;
  };

  [megaSlider, normalSlider, surveySlider, referSlider].forEach((slider) => {
    if (slider) slider.addEventListener('input', updateCalculator);
  });
  updateCalculator();

  document.querySelectorAll('.faq-trigger').forEach((btn) => {
    btn.addEventListener('click', () => {
      const parent = btn.closest('.faq-item');
      if (parent) {
        parent.classList.toggle('open');
      }
    });
  });
});
