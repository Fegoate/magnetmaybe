function rcs_app()
%RCS_APP Visualize bistatic RCS for a missile/rocket target.
%   Launches a simple UI for setting geometric and RF parameters,
%   performs frequency and direction interpolation, and renders
%   the resulting bistatic RCS surface. Coordinate transforms
%   from geocentric to body coordinates are included so that
%   transmitter/receiver geometry can be expressed in lat/lon/alt.
%
%   Run this file from MATLAB or Octave with the command:
%       rcs_app
%
%   The implementation intentionally keeps the physics simple but
%   highlights the workflows required in concept demonstration.

    params = default_params();
    state = struct('interp', [], 'cube', [], 'freqGrid', [], 'azGrid', [], ...
                   'elGrid', [], 'lastAngles', [], 'bistaticAngle', 0);

    fig = uifigure('Name', 'Bistatic RCS Explorer', 'Position', [100 100 1200 700]);
    gl = uigridlayout(fig, [2, 2]);
    gl.ColumnWidth = {330, '1x'};
    gl.RowHeight = {'1x', 180};

    controlPanel = uipanel(gl, 'Title', '参数设置');
    controlPanel.Layout.Row = 1;
    controlPanel.Layout.Column = 1;
    controlLayout = uigridlayout(controlPanel, [9, 2]);
    controlLayout.RowHeight = repmat({28}, 1, 9);
    controlLayout.ColumnWidth = {150, 150};

    % Frequency controls
    uilabel(controlLayout, 'Text', '工作频率 (GHz)');
    freqSlider = uislider(controlLayout, 'Limits', [2 18], ...
        'Value', params.freqGHz, 'MajorTicks', 2:2:18);
    freqSlider.Layout.Row = 1;
    freqSlider.Layout.Column = [1 2];

    uilabel(controlLayout, 'Text', '频率范围 (GHz) [min max]');
    freqRangeField = uieditfield(controlLayout, 'text', ...
        'Value', sprintf('%.1f %.1f', params.freqRangeGHz));

    % Geometry controls
    uilabel(controlLayout, 'Text', '弹体长度 (m)');
    lengthField = uieditfield(controlLayout, 'numeric', ...
        'Value', params.lengthMeters, 'Limits', [1 30]);

    uilabel(controlLayout, 'Text', '弹体直径 (m)');
    diameterField = uieditfield(controlLayout, 'numeric', ...
        'Value', params.diameterMeters, 'Limits', [0.2 5]);

    uilabel(controlLayout, 'Text', '头锥半径 (m)');
    noseField = uieditfield(controlLayout, 'numeric', ...
        'Value', params.noseRadiusMeters, 'Limits', [0.05 3]);

    uilabel(controlLayout, 'Text', '姿态 [偏航 俯仰 翻滚] (deg)');
    attitudeField = uieditfield(controlLayout, 'text', ...
        'Value', sprintf('%.1f %.1f %.1f', params.attitudeYawPitchRollDeg));

    uilabel(controlLayout, 'Text', '目标坐标 [纬度 经度 高度km]');
    tgtField = uieditfield(controlLayout, 'text', ...
        'Value', sprintf('%.2f %.2f %.2f', params.targetLlh));

    uilabel(controlLayout, 'Text', '发射机坐标 [纬度 经度 高度km]');
    txField = uieditfield(controlLayout, 'text', ...
        'Value', sprintf('%.2f %.2f %.2f', params.txLlh));

    uilabel(controlLayout, 'Text', '接收机坐标 [纬度 经度 高度km]');
    rxField = uieditfield(controlLayout, 'text', ...
        'Value', sprintf('%.2f %.2f %.2f', params.rxLlh));

    computeBtn = uibutton(controlLayout, 'Text', '计算并绘制', ...
        'ButtonPushedFcn', @compute_callback);
    computeBtn.Layout.Row = 9;
    computeBtn.Layout.Column = [1 2];

    % Axes for plotting
    surfaceAxes = uiaxes(gl, 'Title', '方向插值后的 RCS (dBsm)');
    surfaceAxes.Layout.Row = 1;
    surfaceAxes.Layout.Column = 2;
    xlabel(surfaceAxes, '方位角 (deg)');
    ylabel(surfaceAxes, '俯仰角 (deg)');
    zlabel(surfaceAxes, 'RCS (dBsm)');

    freqAxes = uiaxes(gl, 'Title', '频率插值曲线 (dBsm)');
    freqAxes.Layout.Row = 2;
    freqAxes.Layout.Column = [1 2];
    xlabel(freqAxes, '频率 (GHz)');
    ylabel(freqAxes, 'RCS (dBsm)');

    resultLabel = uilabel(gl, 'Text', '尚未计算', 'FontWeight', 'bold');
    resultLabel.Layout.Row = 2;
    resultLabel.Layout.Column = 1;

    % Perform one computation on startup
    compute_callback();

    % Nested callback captures UI handles
    function compute_callback(~, ~)
        params.freqGHz = freqSlider.Value;
        params.freqRangeGHz = parse_range(freqRangeField.Value, params.freqRangeGHz);
        params.lengthMeters = lengthField.Value;
        params.diameterMeters = diameterField.Value;
        params.noseRadiusMeters = noseField.Value;
        params.attitudeYawPitchRollDeg = parse_range(attitudeField.Value, params.attitudeYawPitchRollDeg);
        params.targetLlh = parse_range(tgtField.Value, params.targetLlh);
        params.txLlh = parse_range(txField.Value, params.txLlh);
        params.rxLlh = parse_range(rxField.Value, params.rxLlh);

        params = sanitize_params(params);

        % Rebuild interpolation grid when ranges change
        [state.interp, state.cube, state.freqGrid, state.azGrid, state.elGrid] = ...
            build_interpolant(params);

        geom = make_geometry(params);
        [incVecEcef, scaVecEcef] = bistatic_vectors(geom);
        state.bistaticAngle = acosd(dot(incVecEcef, scaVecEcef));

        % Transform to body coordinates and extract angles
        R_ecef_to_body = ecef_to_body_rotation(params.attitudeYawPitchRollDeg);
        incBody = R_ecef_to_body * incVecEcef(:);
        scaBody = R_ecef_to_body * scaVecEcef(:);
        [incAz, incEl] = vector_to_angles(incBody);
        [scaAz, scaEl] = vector_to_angles(scaBody);
        state.lastAngles = [incAz, incEl, scaAz, scaEl];

        % Evaluate RCS at selected frequency
        sigma = bistatic_rcs(state.interp, params.freqGHz, incAz, incEl, scaAz, scaEl);
        sigmaDb = 10 * log10(sigma);

        % Directional surface at the selected frequency
        [AZ, EL] = meshgrid(state.azGrid, state.elGrid);
        freqMatrix = params.freqGHz * ones(size(AZ));
        sliceLinear = state.interp(freqMatrix, AZ, EL);
        sliceDb = 10 * log10(sliceLinear + eps);

        surf(surfaceAxes, AZ, EL, sliceDb, 'EdgeAlpha', 0.2);
        view(surfaceAxes, 40, 35);
        colorbar(surfaceAxes);
        hold(surfaceAxes, 'on');
        plot3(surfaceAxes, incAz, incEl, sigmaDb, 'rp', 'MarkerSize', 12, 'LineWidth', 2);
        plot3(surfaceAxes, scaAz, scaEl, sigmaDb, 'g^', 'MarkerSize', 10, 'LineWidth', 2);
        hold(surfaceAxes, 'off');

        % Frequency interpolation plot at fixed directions
        freqSweep = linspace(params.freqRangeGHz(1), params.freqRangeGHz(2), 100);
        sweepSigma = state.interp(freqSweep, incAz * ones(size(freqSweep)), incEl * ones(size(freqSweep)));
        sweepSigma = sweepSigma .* bistatic_gain(state.bistaticAngle, incEl, scaEl);
        plot(freqAxes, freqSweep, 10 * log10(sweepSigma + eps), 'LineWidth', 2);
        grid(freqAxes, 'on');

        % Text output
        resultLabel.Text = sprintf(['入射角: %.1f/%.1f deg, 散射角: %.1f/%.1f deg\n' ...
            '双站夹角: %.1f deg, RCS(%.2f GHz): %.2f dBsm'], ...
            incAz, incEl, scaAz, scaEl, state.bistaticAngle, params.freqGHz, sigmaDb);
    end
end

function params = default_params()
%DEFAULT_PARAMS Basic defaults for the UI fields.
    params.freqGHz = 10;
    params.freqRangeGHz = [3 16];
    params.lengthMeters = 6;
    params.diameterMeters = 0.8;
    params.noseRadiusMeters = 0.25;
    params.attitudeYawPitchRollDeg = [20 5 -10];
    params.targetLlh = [25, 115, 0.2];
    params.txLlh = [26, 114, 0.1];
    params.rxLlh = [24.5, 115.5, 0.1];
end

function [interpObj, cube, freqGrid, azGrid, elGrid] = build_interpolant(params)
%BUILD_INTERPOLANT Construct the frequency/direction interpolant cube.
    freqGrid = linspace(params.freqRangeGHz(1), params.freqRangeGHz(2), 9);
    azGrid = linspace(0, 360, 73);     % 5-degree spacing
    elGrid = linspace(-80, 80, 33);

    cube = zeros(length(freqGrid), length(azGrid), length(elGrid));
    for fi = 1:numel(freqGrid)
        for ai = 1:numel(azGrid)
            for ei = 1:numel(elGrid)
                cube(fi, ai, ei) = synthetic_rcs(freqGrid(fi), azGrid(ai), elGrid(ei), params);
            end
        end
    end

    interpObj = griddedInterpolant({freqGrid, azGrid, elGrid}, cube, 'pchip', 'nearest');
end

function sigma = synthetic_rcs(freqGHz, azDeg, elDeg, params)
%SYNTHETIC_RCS Simplified missile/rocket RCS model.
%   Combines a slender-body term with a nose-tip highlight and a
%   roll-sensitive lobe for directionality. Units are linear square meters.
    c = 2.99792458e8;
    lambda = c / (freqGHz * 1e9);

    radius = params.diameterMeters / 2;
    area = pi * radius ^ 2;
    slender = (4 * pi * area ^ 2) / lambda ^ 2;
    elevationGain = cosd(elDeg) .^ 2;

    noseSpot = (params.noseRadiusMeters / lambda) ^ 2 * exp(-(abs(elDeg) / 25) .^ 2);
    rollLobe = 0.6 + 0.4 * cosd(azDeg) .^ 2;
    azGradient = 1 + 0.1 * cosd(2 * azDeg);

    sigma = (slender .* elevationGain .* rollLobe + noseSpot) .* azGradient;
    sigma = max(sigma, 1e-6);
end

function params = sanitize_params(params)
%SANITIZE_PARAMS Clamp and tidy user input.
    params.freqRangeGHz = max(params.freqRangeGHz, 0.1);
    params.freqRangeGHz(2) = max(params.freqRangeGHz(2), params.freqRangeGHz(1) + 0.1);
    params.freqGHz = min(max(params.freqGHz, params.freqRangeGHz(1)), params.freqRangeGHz(2));
    params.lengthMeters = max(params.lengthMeters, 0.5);
    params.diameterMeters = max(params.diameterMeters, 0.1);
    params.noseRadiusMeters = max(params.noseRadiusMeters, 0.05);
end

function arr = parse_range(textValue, default)
%PARSE_RANGE Parse a whitespace-separated numeric row vector.
    try
        vals = str2num(textValue); %#ok<ST2NM>
        if isempty(vals)
            arr = default;
        else
            arr = vals(:).';
        end
    catch
        arr = default;
    end
end

function geom = make_geometry(params)
%MAKE_GEOMETRY Compute ECEF locations for TX/RX/target.
    geom.target = llh_to_ecef(params.targetLlh);
    geom.tx = llh_to_ecef(params.txLlh);
    geom.rx = llh_to_ecef(params.rxLlh);
end

function [incVecEcef, scaVecEcef] = bistatic_vectors(geom)
%BISTATIC_VECTORS Propagation vectors in ECEF coordinates.
    incVecEcef = geom.target - geom.tx;  % toward target
    scaVecEcef = geom.rx - geom.target;  % toward receiver
    incVecEcef = incVecEcef ./ norm(incVecEcef);
    scaVecEcef = scaVecEcef ./ norm(scaVecEcef);
end

function R = ecef_to_body_rotation(yawPitchRollDeg)
%ECEF_TO_BODY_ROTATION Rotation matrix from ECEF to body axes.
    yaw = deg2rad(yawPitchRollDeg(1));
    pitch = deg2rad(yawPitchRollDeg(2));
    roll = deg2rad(yawPitchRollDeg(3));

    Rz = [cos(yaw), -sin(yaw), 0; sin(yaw), cos(yaw), 0; 0, 0, 1];
    Ry = [cos(pitch), 0, sin(pitch); 0, 1, 0; -sin(pitch), 0, cos(pitch)];
    Rx = [1, 0, 0; 0, cos(roll), -sin(roll); 0, sin(roll), cos(roll)];

    % Body-321 rotation. To map ECEF vectors into body frame use transpose.
    R = (Rz * Ry * Rx).';
end

function [azDeg, elDeg] = vector_to_angles(v)
%VECTOR_TO_ANGLES Convert a 3D vector to azimuth/elevation angles.
    v = v(:) ./ max(norm(v), eps);
    azDeg = mod(rad2deg(atan2(v(2), v(1))), 360);
    elDeg = rad2deg(asin(v(3)));
end

function ecef = llh_to_ecef(llh)
%LLH_TO_ECEF Convert [lat lon h(km)] to ECEF meters.
    lat = deg2rad(llh(1));
    lon = deg2rad(llh(2));
    h = llh(3) * 1e3;

    a = 6378137.0;
    e2 = 6.69437999014e-3;
    N = a ./ sqrt(1 - e2 * (sin(lat) .^ 2));

    x = (N + h) .* cos(lat) .* cos(lon);
    y = (N + h) .* cos(lat) .* sin(lon);
    z = (N * (1 - e2) + h) .* sin(lat);
    ecef = [x; y; z];
end

function sigma = bistatic_rcs(interpObj, freqGHz, incAz, incEl, scaAz, scaEl)
%BISTATIC_RCS Evaluate bistatic RCS with interpolation and gain factor.
    effAz = wrap_to_360((incAz + scaAz) / 2);
    effEl = (incEl + scaEl) / 2;
    monostatic = interpObj(freqGHz, effAz, effEl);
    sigma = monostatic .* bistatic_gain(acosd_direction(incAz, incEl, scaAz, scaEl), incEl, scaEl);
end

function gain = bistatic_gain(bistaticAngleDeg, incEl, scaEl)
%BISTATIC_GAIN Simple weighting for bistatic separation and elevations.
    coherence = (1 + cosd(bistaticAngleDeg)) / 2;
    elevationTerm = (cosd(incEl) .^ 2 + cosd(scaEl) .^ 2) / 2;
    gain = max(coherence .* elevationTerm, 1e-3);
end

function ang = acosd_direction(incAz, incEl, scaAz, scaEl)
%ACOSD_DIRECTION Approximate bistatic angle between two look directions.
    vi = angles_to_unit(incAz, incEl);
    vs = angles_to_unit(scaAz, scaEl);
    ang = acosd(dot(vi, vs));
end

function ang = wrap_to_360(ang)
%WRAP_TO_360 Wrap angle to [0, 360).
    ang = mod(ang, 360);
    ang(ang < 0) = ang(ang < 0) + 360;
end

function v = angles_to_unit(azDeg, elDeg)
%ANGLES_TO_UNIT Unit vector from azimuth/elevation.
    az = deg2rad(azDeg);
    el = deg2rad(elDeg);
    v = [cos(el) .* cos(az); cos(el) .* sin(az); sin(el)];
end
